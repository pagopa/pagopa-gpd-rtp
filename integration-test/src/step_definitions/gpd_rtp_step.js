const assert = require('assert');
const { After, Given, When, Then, setDefaultTimeout, AfterAll } = require('@cucumber/cucumber');
const { sleep } = require("./common");
const { readFromRedisWithKey, shutDownClient } = require("./redis_client");
const { readFromOptInRedisWithKey, writeOnOptInRedisKeyValue, shutDownOptInRedisClient } = require("./opt_in_redis_client");
const { shutDownPool, insertPaymentPosition, updatePaymentPosition, deletePaymentPosition, insertPaymentOption, deletePaymentOption, insertTransfer, deleteTransfer } = require("./pg_gpd_client");

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

AfterAll(async function () {
  shutDownPool();
  shutDownClient();
  shutDownOptInRedisClient();
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

Given('an EC with fiscal code {string} and flag opt in enabled on Redis cache', function (fiscalCode) {
  const flags = readFromOptInRedisWithKey("rtp_flag_optin");
  if (!flags.contains(fiscalCode)) {
    flags.put(fiscalCode);
    writeOnOptInRedisKeyValue("rtp_flag_optin", flags);
  }
});

Given('a payment position with id {string} and fiscal code {string} in GPD database', async function (id, fiscalCode) {
  await insertPaymentPosition(id, fiscalCode);
  this.paymentPositionId = id;
  this.paymentPositionFiscalCode = fiscalCode;
});


Given('a payment option with id {string} and associated to payment position with id {string} in GPD database', async function (id, paymentPositionId) {
  await insertPaymentOption(id, paymentPositionId);
  this.paymentOptionId = id;
});


Given('a create operation on transfer table with id {string}, category {string}, remittance information {string} and associated to payment option with id {string} in GPD database', async function (id, category, remittanceInformation, paymentOptionId) {
  await insertTransfer(id, category, remittanceInformation, paymentOptionId);
  this.transferId = id;
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


Then('the RTP topic returns the {string} operation with id {string}', async function (operation, id) {
  let operationMessage = await readFromRedisWithKey(id);
  let po = JSON.parse(operationMessage).value;
  if (operation === "create") {
    this.rtpCreateOp = po;
    assert.strictEqual(this.rtpCreateOp.operation, "CREATE");
  } else if (operation === "update") {
    this.rtpUpdateOp = po;
    assert.strictEqual(this.rtpUpdateOp.operation, "UPDATE");
  } else if (operation === "delete") {
    this.rtpDeleteOp = po;
    assert.strictEqual(this.rtpDeleteOp.operation, "DELETE");
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
