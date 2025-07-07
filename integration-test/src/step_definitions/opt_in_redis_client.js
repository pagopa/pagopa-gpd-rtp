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

async function fiscalCodeIsPresentInOptInRedisCache(fiscalCode) {
    return await client.sIsMember("rtp_flag_optin", fiscalCode);
}

async function addFiscalCodeInOptInRedisCache(fiscalCode) {
    return await client.sAdd("rtp_flag_optin", fiscalCode);
}

async function shutDownOptInRedisClient() {
    await client.quit();
  }

module.exports = {
    fiscalCodeIsPresentInOptInRedisCache, addFiscalCodeInOptInRedisCache, shutDownOptInRedisClient
  }