const { Kafka } = require('kafkajs')
const { createClient } = require('redis');

const evhHost = process.env.INGESTION_EVENTHUB_HOST;
const evhConnectionString = process.env.INGESTION_EVENTHUB_CONN_STRING;
const evhTopics = process.env.INGESTION_EVENTHUB_TOPICS.split(',');
const redisHost = process.env.REDIS_HOST;
const redisPort = process.env.REDIS_PORT;

async function eventHubToRedisHandler() {
    try {
        console.log("TOPICS ", evhTopics);
        const kafka = new Kafka({
            clientId: 'kafka-to-redis-app', brokers: [evhHost],   //
            authenticationTimeout: 10000, //
            reauthenticationThreshold: 10000,
            ssl: true,
            sasl: {
                mechanism: 'plain', // scram-sha-256 or scram-sha-512
                username: '$ConnectionString',
                password: evhConnectionString
            },
        })
        // Connect to Kafka broker
        const consumer = kafka.consumer({ groupId: 'gpd-rtp-integration-test-consumer-group' });
        await consumer.connect();
        await consumer.subscribe({ topics: evhTopics })
        // Create Redis client
        const client = createClient({
            socket: {
                port: redisPort,
                host: redisHost
            }
        });
        client.on('error', err => console.log('Redis Client Error', err))
        await client.connect();

        client.on('connect', function () {
            console.log('Connected!');
        });


        // Listen to the topic
        let decoder = new TextDecoder("utf-8");
        await consumer.run({
            eachMessage: async ({ topic, partition, message, heartbeat, pause }) => {
                if(message && message.value){
                    writeOnRedis(client, decoder, message, topic)
                }
            },
        })

        // when call client close?
        // await client.quit();
    } catch (e) {
        console.error(e);
    }
}

async function writeOnRedis(client, decoder, message, topic) {
    let messageBody = decoder.decode(message.value);
    let decodedMessageBody = JSON.parse(messageBody);
    let id = getEventId(decodedMessageBody, topic);
    message.value = decodedMessageBody;
    await client.set(id, JSON.stringify(message));
}

function getEventId(event, topic) {
    if (event.operation === "CREATE") {
        return event.after.id + `-c`;
    } else if (event.operation === "DELETE") {
        return event.before.id + `-d`;
    } else {
        return event.after.id + `-u`;
    }
}

eventHubToRedisHandler();
