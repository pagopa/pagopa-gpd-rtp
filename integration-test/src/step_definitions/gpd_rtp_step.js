const assert = require('assert');
const { After, Given, When, Then, setDefaultTimeout, AfterAll, BeforeAll } = require('@cucumber/cucumber');
const { sleep, getRandomInt } = require("./common");
const { fiscalCodeIsPresentInOptInRedisCache, addFiscalCodeInOptInRedisCache, shutDownOptInRedisClient } = require("./opt_in_redis_client");
const { shutDownPool, insertPaymentPosition, updatePaymentOption, deletePaymentPosition, insertPaymentOption, deletePaymentOption, insertTransfer, deleteTransfer } = require("./pg_gpd_client");
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

BeforeAll(async function () {
  await eventHubToMemoryHandler();
});

AfterAll(async function () {
  await shutDownPool();
  await shutDownOptInRedisClient();
  await shutDownKafka();
});

// After each Scenario
After(async function () {
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
});


Given('an EC with fiscal code {string} and flag opt in enabled on Redis cache', async function (fiscalCode) {
  const exist = await fiscalCodeIsPresentInOptInRedisCache(fiscalCode);

  if (!exist) {
    await addFiscalCodeInOptInRedisCache(fiscalCode);
  }
});

Given('a create payment position with id prefix {string} and fiscal code {string} on GPD database', async function (id, fiscalCode) {
  this.paymentPositionId = id * 10000 + getRandomInt();
  await insertPaymentPosition(this.paymentPositionId, fiscalCode);
  this.paymentPositionFiscalCode = fiscalCode;
});

Given('a create payment option with id prefix {string} and associated to the previous payment position on GPD database', async function (id) {
  this.paymentOptionId = id * 10000 + getRandomInt();
  await insertPaymentOption(this.paymentOptionId, this.paymentPositionId, this.paymentPositionFiscalCode);
});

Given('a create transfer with id prefix {string}, category {string}, remittance information {string} and associated to the previous payment option on GPD database', async function (id, category, remittanceInformation) {
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
  await sleep(time);
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

Then('the {string} operation has the remittance information anonymized', function (operation) {
  if (operation === "create") {
    assert.notStrictEqual(this.rtpCreateOp.subject, undefined);
    assert.notStrictEqual(this.rtpCreateOp.subject, this.remittanceInformation);
  } else if (operation === "update") {
    assert.notStrictEqual(this.rtpUpdateOp.subject, undefined);
    assert.notStrictEqual(this.rtpUpdateOp.subject, this.remittanceInformation);
  }
});

Then('the create operation has the status {string}', function (status) {
  assert.strictEqual(this.rtpCreateOp.status, status);
});

Then('the update operation has the description {string}', function (description) {
  assert.strictEqual(this.paymentOptionUpdatedDescription, description);
  assert.strictEqual(this.rtpUpdateOp.description, description);
});
