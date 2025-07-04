const { Kafka } = require('kafkajs');

const evhHost = process.env.RTP_EVENTHUB_HOST;
const evhConnectionString = process.env.RTP_EVENTHUB_CONN_STRING;
const evhTopics = process.env.RTP_EVENTHUB_TOPICS.split(',');

if (!evhConnectionString || typeof evhConnectionString !== 'string') {
  throw new Error('Kafka password is invalid');
}


let consumer;
let decoder = new TextDecoder("utf-8");

// In-memory store
const inMemoryStore = new Map();

async function eventHubToMemoryHandler() {
    try {
        const kafka = new Kafka({
            clientId: 'kafka-to-memory-app',
            brokers: [evhHost],
            authenticationTimeout: 10000,
            reauthenticationThreshold: 10000,
            ssl: true,
            sasl: {
                mechanism: 'plain',
                username: '$ConnectionString',
                password: evhConnectionString
            },
        });

        consumer = kafka.consumer({ groupId: 'gpd-rtp-integration-test-consumer-group' });
        await consumer.connect();
        await consumer.subscribe({ topics: evhTopics });

        console.log('✅ Connected to Kafka');

        await consumer.run({
            eachMessage: async ({ topic, partition, message }) => {
                if (message?.value) {
                    await writeInMemory(message);
                }
            },
        });

    } catch (err) {
        console.error('❌ Error in handler:', err);
        await shutDownKafka();
    }
}

async function writeInMemory(message) {
    try {
        const messageBody = decoder.decode(message.value);
        const decodedMessageBody = JSON.parse(messageBody);
        const id = getEventId(decodedMessageBody);
        inMemoryStore.set(id, decodedMessageBody);
    } catch (err) {
        console.error('❌ Error storing message:', err);
    }
}

function getEventId(event) {
    let suffix;
    switch (event.operation) {
        case "CREATE":
            suffix = "-c"; break;
        case "DELETE":
            suffix = "-d"; break;
        default:
            suffix = "-u"; break;
    }
    return `${event.id}${suffix}`;
}

async function shutDownKafka() {
    try {
        if (consumer) {
            await consumer.stop();
            await consumer.disconnect();
            console.log('🛑 Kafka consumer disconnected');
        }
    } catch (err) {
        console.error('❌ Error during shutdown:', err);
    }
}

function getStoredMessage(id) {
    return inMemoryStore.get(id);
}

module.exports = {
    eventHubToMemoryHandler,
    shutDownKafka,
    getStoredMessage
};
