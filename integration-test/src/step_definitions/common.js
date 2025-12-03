const {getStoredMessage} = require("./kafka_event_hub_client");
const {insertPaymentPosition} = require("./pg_gpd_client");

function sleep(ms) {
	return new Promise(resolve => setTimeout(resolve, ms));
}

function getRandomInt() {
  return Math.floor(1000 + Math.random() * 9000);
}

async function getPaymentOptionInTime(paymentOptionId, suffix, timeout) {
    const startTime = new Date().getTime();
    const pollInterval = 100; // Check every 100 milliseconds
    let foundOperation = false;
    let po;

    while (new Date().getTime() - startTime < timeout && !foundOperation) {
        po = getStoredMessage(`${paymentOptionId}-${suffix}`);

        if (po !== undefined) {
            console.log(`✅ Event found for ${paymentOptionId}-${suffix} after ${new Date().getTime() - startTime}ms.`);
            foundOperation = true;
        }
        else {
            // Wait for the next poll interval
            await sleep(pollInterval);
        }
    }

    return [foundOperation, po];
}

async function createPaymentPosition(id, fiscalCode, debtPositionStatus) {
    let paymentPositionId = id * 10000 + getRandomInt();
    console.log("Creating payment position with id prefix ", paymentPositionId);
    await insertPaymentPosition(paymentPositionId, fiscalCode, debtPositionStatus);
    return paymentPositionId;
}

module.exports = {
 sleep, getRandomInt, getPaymentOptionInTime, createPaymentPosition
}