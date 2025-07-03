const assert = require('assert');
const { After, Given, When, Then, setDefaultTimeout, AfterAll, BeforeAll } = require('@cucumber/cucumber');
const { sleep, getRandomInt } = require("./common");
const { fiscalCodeIsPresentInOptInRedisCache, addFiscalCodeInOptInRedisCache, shutDownOptInRedisClient } = require("./opt_in_redis_client");
const { shutDownPool, insertPaymentPosition, updatePaymentPosition, deletePaymentPosition, insertPaymentOption, deletePaymentOption, insertTransfer, deleteTransfer } = require("./pg_gpd_client");
const { eventHubToMemoryHandler, shutDownKafka, getStoredMessage } = require("./kafka_event_hub_client");

// set timeout for Hooks function, it allows to wait for long task
setDefaultTimeout(360 * 1000);

// initialize variables

////////////////////////////
// Payment Positions vars //
////////////////////////////
this.paymentPositionId = null;
this.paymentPositionFiscalCode = null;
this.paymentPositionUpdatedStatus = null;

///////////////////////////
// Payment Options vars  //
///////////////////////////
this.paymentOptionId = null;
this.rtpCreateOp = null;
this.rtpUpdateOp = null;
this.rtpDeleteOp = null;

////////////////////
// Transfer vars  //
////////////////////
this.transferId = null;
this.transferCategory = null;
this.remittanceInformation = null;

BeforeAll(async function () {
  eventHubToMemoryHandler();
});

AfterAll(async function () {
  shutDownPool();
  shutDownOptInRedisClient();
  shutDownKafka();
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
  this.paymentPositionUpdatedStatus = null;

  ///////////////////////////
  // Payment Options vars  //
  ///////////////////////////
  this.paymentOptionId = null;
  this.rtpCreateOp = null;
  this.rtpUpdateOp = null;
  this.rtpDeleteOp = null;

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
  this.paymentPositionId = id * 100000 + getRandomInt();
  await insertPaymentPosition(this.paymentPositionId, fiscalCode);
  this.paymentPositionFiscalCode = fiscalCode;
});


Given('a create payment option with id prefix {string} and associated to the previous payment position on GPD database', async function (id) {
  this.paymentOptionId = id * 100000 + getRandomInt();;
  await insertPaymentOption(this.paymentOptionId, this.paymentPositionId, this.paymentPositionFiscalCode);
});


Given('a create transfer with id prefix {string}, category {string}, remittance information {string} and associated to the previous payment option on GPD database', async function (id, category, remittanceInformation) {
  this.transferId = id * 100000 + getRandomInt();
  await insertTransfer(this.transferId, category, remittanceInformation, this.paymentOptionId);
  this.transferCategory = category;
  this.remittanceInformation = remittanceInformation;
});


Given('an update operation on field status with new value {string} on the same payment position in GPD database', async function (status) {
  await updatePaymentPosition(this.paymentOptionId, status);
  this.paymentPositionUpdatedStatus = status;
});

Given('a delete operation on the same transfer in GPD database', async function () {
  await deleteTransfer(this.transferId);
});

Given('a delete operation on the same payment option in GPD database', async function () {
  await deletePaymentOption(this.paymentOptionId);
});


When('the operations have been properly published on RTP event hub after {int} ms', async function (time) {
  // boundary time spent by azure function to process event
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

Then('the create operation has the remittance information anonymized', function () {
  assert.notStrictEqual(this.rtpCreateOp.subject, undefined);
  assert.notStrictEqual(this.rtpCreateOp.subject, this.remittanceInformation);
});

Then('the create operation has the status {string}', function (status) {
  assert.strictEqual(this.rtpCreateOp.status, status);
});

Then('the update operation has the status {string}', function (status) {
  assert.strictEqual(this.paymentPositionUpdatedStatus, status);
  assert.strictEqual(this.rtpUpdateOp.status, status);
});
