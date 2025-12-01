const assert = require('assert');
const { Before,    After, Given, When, Then, setDefaultTimeout, AfterAll, BeforeAll } = require('@cucumber/cucumber');
const { sleep, getRandomInt, getPaymentOptionInTime, createPaymentPosition} = require("./common");
const { fiscalCodeIsPresentInOptInRedisCache, addFiscalCodeInOptInRedisCache, shutDownOptInRedisClient } = require("./opt_in_redis_client");
const { shutDownPool, insertPaymentPosition, updatePaymentOption, deletePaymentPosition, insertPaymentOption, deletePaymentOption, insertTransfer, deleteTransfer,
  deletePaymentPositionByIUPD
} = require("./pg_gpd_client");
const { eventHubToMemoryHandler, shutDownKafka, getStoredMessage } = require("./kafka_event_hub_client");

// set timeout for Hooks function, it allows to wait for long task
setDefaultTimeout(360 * 1000);

// initialize variables

////////////////////////////
// Payment Positions vars //
////////////////////////////
this.paymentPositionId = null;
this.paymentPositionFiscalCode = null;

///////////////////////////
// Payment Options vars  //
///////////////////////////
this.paymentOptionId = null;
this.rtpCreateOp = null;
this.rtpUpdateOp = null;
this.rtpDeleteOp = null;
this.paymentOptionUpdatedDescription = null;

////////////////////
// Transfer vars  //
////////////////////
this.transferId = null;
this.transferCategory = null;
this.remittanceInformation = null;
this.description = null;

BeforeAll(async function () {
  await eventHubToMemoryHandler();
});

BeforeAll({tags: '@clean-up-required'}, async function () {
  await deletePaymentPositionByIUPD('IUPD_INTEGRATION_TEST_GPD_RTP')
})

AfterAll(async function () {
  console.log("Shutdown pool")
  await shutDownPool();
  console.log("Shutdown redis client")
  await shutDownOptInRedisClient();
  console.log("Shutdown kafka client")
  await shutDownKafka();
});

Before({tags: '@create-payment-position-required'}, async function () {
  // create payment position
  const id = 123121;
  this.paymentPositionFiscalCode = '77777777777';
  this.paymentPositionId = await createPaymentPosition(id, this.paymentPositionFiscalCode);

  // create payment option
  this.paymentOptionId = id * 10000 + getRandomInt();
  this.description = "before scenario";
  await insertPaymentOption(this.paymentOptionId, this.paymentPositionId, this.paymentPositionFiscalCode, this.description);

  // create transfer
  this.transferId = id * 10000 + getRandomInt();
  this.remittanceInformation = '/RFB/091814449948492/547.24/TXT/DEBITORE/VNTMHL76M09H501D';
  await insertTransfer(this.transferId, '9/0101100IM/', this.remittanceInformation, this.paymentOptionId);
})

// After each Scenario
After({tags: '@clean-up-required'}, async function () {
  // remove event
  if (this.transferId != null) {
    await deleteTransfer(this.transferId);
  }
  if (this.paymentOptionId != null) {
    await deletePaymentOption(this.paymentOptionId);
  }
  if (this.paymentPositionId != null) {
    await deletePaymentPosition(this.paymentPositionId);
  }

  ////////////////////////////
  // Payment Positions vars //
  ////////////////////////////
  this.paymentPositionId = null;
  this.paymentPositionFiscalCode = null;

  ///////////////////////////
  // Payment Options vars  //
  ///////////////////////////
  this.paymentOptionId = null;
  this.rtpCreateOp = null;
  this.rtpUpdateOp = null;
  this.rtpDeleteOp = null;
  this.paymentOptionUpdatedDescription = null;

  ////////////////////
  // Transfer vars  //
  ////////////////////
  this.transferId = null;
  this.transferCategory = null;
  this.remittanceInformation = null;
  this.description = null;
});


Given('an EC with fiscal code {string} and flag opt in enabled on Redis cache', async function (fiscalCode) {
  const exist = await fiscalCodeIsPresentInOptInRedisCache(fiscalCode);

  if (!exist) {
    await addFiscalCodeInOptInRedisCache(fiscalCode);
  }
});

Given('a create payment position with id prefix {string} and fiscal code {string} on GPD database', async function (id, fiscalCode) {
  // this.paymentPositionId = id * 10000 + getRandomInt();
  // console.log("Creating payment position with id prefix {string}", this.paymentPositionId);
  // await insertPaymentPosition(this.paymentPositionId, fiscalCode);
  this.paymentPositionFiscalCode = fiscalCode;
  this.paymentPositionId = await createPaymentPosition(fiscalCode);
});

Given('a create payment option with id prefix {string}, description {string} and associated to the previous payment position on GPD database', async function (id, descriptionString) {
  this.paymentOptionId = id * 10000 + getRandomInt();
  this.description = descriptionString;
  await insertPaymentOption(this.paymentOptionId, this.paymentPositionId, this.paymentPositionFiscalCode, this.description);
});

Given('a create transfer with id prefix {string}, category {string}, remittance information of primary ec {string} and associated to the previous payment option on GPD database', async function (id, category, remittanceInformation) {
  this.transferId = id * 10000 + getRandomInt();
  await insertTransfer(this.transferId, category, remittanceInformation, this.paymentOptionId);
  this.transferCategory = category;
  this.remittanceInformation = remittanceInformation;
});

Given('an update operation on field description with new value {string} on the same payment option in GPD database', async function (description) {
  await updatePaymentOption(this.paymentOptionId, description);
  this.paymentOptionUpdatedDescription = description;
});

Given('a delete operation on the same transfer in GPD database', async function () {
  await deleteTransfer(this.transferId);
});

Given('a delete operation on the same payment option in GPD database', async function () {
  await deletePaymentOption(this.paymentOptionId);
});

Given('the create operation has been properly published on RTP event hub after {int} ms', async function (time) {
  // boundary time spent to process event
  await sleep(time);
});

When('the operations have been properly published on RTP event hub after {int} ms', async function (time) {
  // boundary time spent to process event
  console.log("Sleeping for ", time);
  await sleep(time);
});

When('in the RTP topic does not exist a {string} operation with id suffix {string} in {int} ms', async function (operation, suffix, timeout) {
  [foundOperation, po] = await getPaymentOptionInTime(this.paymentOptionId, suffix, timeout);
  assert.strictEqual(foundOperation, false)
});

When('in the RTP topic exists a {string} operation with id suffix {string} in {int} ms', async function (operation, suffix, timeout) {
  [foundOperation, po] = await getPaymentOptionInTime(this.paymentOptionId, suffix, timeout);
  if (foundOperation) {
    if (operation === "create") {
      this.rtpCreateOp = po;
      assert.strictEqual(this.rtpCreateOp.operation, "CREATE");
    } else if (operation === "update") {
      this.rtpUpdateOp = po;
      assert.strictEqual(this.rtpUpdateOp.operation, "UPDATE");
    } else if (operation === "delete") {
      this.rtpDeleteOp = po;
      assert.strictEqual(this.rtpDeleteOp.operation, "DELETE");
    } else {
      console.log(`♠️ Event not found for ${this.paymentOptionId}-${suffix} after ${timeout}ms.`);
      assert.fail("Unexpected operation");
    }

  }
  else {
    console.log(`♠️ Event not found for ${this.paymentOptionId}-${suffix} after ${timeout}ms.`);
    assert.fail("Unexpected operation");
  }
});

Then('the RTP topic returns the {string} operation with id suffix {string}', async function (operation, suffix) {
  let po = getStoredMessage(`${this.paymentOptionId}-${suffix}`);
  if (operation === "create") {
    this.rtpCreateOp = po;
    assert.strictEqual(this.rtpCreateOp.operation, "CREATE");
  } else if (operation === "update") {
    this.rtpUpdateOp = po;
    assert.strictEqual(this.rtpUpdateOp.operation, "UPDATE");
  } else if (operation === "delete") {
    this.rtpDeleteOp = po;
    assert.strictEqual(this.rtpDeleteOp.operation, "DELETE");
  } else {
    assert.fail("Unexpected operation");
  }
});

Then("the {string} RTP message has not been sent through event hub", function (operation) {
  assert.strictEqual(this.rtpCreateOp, undefined);
});

Then("the {string} operation has the first transfer's remittance information", function (operation) {
  if (operation === "create") {
    assert.notStrictEqual(this.rtpCreateOp.subject, undefined);
    assert.notStrictEqual(this.rtpCreateOp.subject, this.remittanceInformation);
    assert.notStrictEqual(this.rtpCreateOp.description, this.description);
  } else if (operation === "update") {
    assert.notStrictEqual(this.rtpUpdateOp.subject, undefined);
    assert.notStrictEqual(this.rtpUpdateOp.subject, this.remittanceInformation);
    assert.notStrictEqual(this.rtpUpdateOp.description, this.description);
  }
});

Then('the create operation has the status {string}', function (status) {
  assert.strictEqual(this.rtpCreateOp.status, status);
});

Then('the update operation has the description {string}', function (description) {
  assert.strictEqual(this.paymentOptionUpdatedDescription, description);
});

Then('the {string} RTP message has the anonymized description {string}', function (operation, description) {
  if(operation === "create"){
    assert.strictEqual(this.rtpCreateOp.description, description);
  } else if(operation === "update"){
    assert.strictEqual(this.rtpUpdateOp.description, description);
  }
});
