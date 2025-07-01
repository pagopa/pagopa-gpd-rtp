const { createClient } = require('redis');

const redisPassword = process.env.OPT_IN_REDIS_PASSWORD;
const redisHost = process.env.OPT_IN_REDIS_HOSTNAME;
const redisPort = process.env.OPT_IN_REDIS_PORT;

const client = createClient({
    socket: {
        port: redisPort,
        host: redisHost,
        tls: true
    },
    password: redisPassword
});

client.on('error', err => console.log('Redis Client Error', err))
client.connect();

client.on('connect', function () {
    console.log('Connected!');
});

async function readFromOptInRedisWithKey(key) {
    return await client.get(key);
}

async function writeOnOptInRedisKeyValue(key, value) {
    return await client.set(key, value);
}

async function shutDownOptInRedisClient() {
    await client.quit();
  }

module.exports = {
    readFromOptInRedisWithKey, writeOnOptInRedisKeyValue, shutDownOptInRedisClient
  }