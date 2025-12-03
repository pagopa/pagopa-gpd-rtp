const { Pool } = require('pg');

const username = process.env.PG_GPD_USERNAME;
const password = process.env.PG_GPD_PASSWORD;
const serverName = process.env.PG_GPD_SERVER_NAME;
const databaseName = process.env.PG_GPD_DATABASE_NAME;

const pool = new Pool({
  user: username,
  database: databaseName,
  password: password,
  host: serverName,
  port: 5432,
  ssl: true
});

const connection = {
  pool,
  query: (...args) => {
    return pool.connect().then((client) => {
      return client.query(...args)
          .then((res) => res.rows)
          .finally(() => client.release());
    });
  },
};

async function shutDownPool() {
  await pool.end();
}

async function insertPaymentPosition(id, fiscalCode, status) {
  await connection.query(`INSERT INTO apd.apd.payment_position (id, city, civic_number, company_name, country, email, fiscal_code, full_name, inserted_date, iupd, last_updated_date, max_due_date, min_due_date, office_name, organization_fiscal_code, phone, postal_code, province, publish_date, region, status, street_name, "type", "version", payment_date, pull, pay_stand_in, service_type) VALUES('${id}', 'Pizzo Calabro', '11', 'SkyLab Inc.', 'IT', 'micheleventimiglia@skilabmail.com', 'VNTMHL76M09H501D', 'Michele Ventimiglia', now(), 'IUPD_INTEGRATION_TEST_GPD_RTP', now(), '2024-12-12 16:09:43.323', '2024-12-12 16:09:43.323', 'SkyLab - Sede via Washington - Edit', '${fiscalCode}', '333-123456789', '89812', 'VV', '2024-11-12 16:09:43.479', 'CA', '${status}', 'via Washington', 'F', 0, NULL, true, false, 'GPD');`);
}

async function deletePaymentPositionByIUPD(iupd) {
  await connection.query(`WITH payment_position_row AS (SELECT id FROM apd.payment_position WHERE iupd = '${iupd}'),
                               payment_options AS (
                                 SELECT id
                                 FROM apd.payment_option
                                 WHERE payment_position_id IN (SELECT id FROM payment_position_row)
                               ),
                               transfers AS (
                                 SELECT id
                                 FROM apd.transfer
                                 WHERE payment_option_id IN (SELECT id FROM payment_options)
                               ),
                               deleted_metadata AS (
                                  DELETE FROM apd.transfer_metadata
                                  WHERE transfer_id IN (SELECT id FROM transfers)
                                    RETURNING *
                                ),
                              deleted_transfers AS (
                                DELETE FROM apd.transfer
                                WHERE id IN (SELECT id FROM transfers)
                                  RETURNING *
                                  ),
                                  deleted_payment_options AS (
                                DELETE FROM apd.payment_option
                                WHERE id IN (SELECT id FROM payment_options)
                                  RETURNING *
                                  )
                                DELETE FROM apd.payment_position
                                WHERE id IN (SELECT id FROM payment_position_row);`);
}

async function deletePaymentPosition(id) {
  await connection.query(`DELETE FROM apd.apd.payment_position WHERE id='${id}'`);
}

async function insertPaymentOption(id, paymentPositionId, ecFiscalCode, description, debtorFiscalCode) {
  const currentDate = new Date();
  const formattedDate = formatDate(currentDate);
  await connection.query(`INSERT INTO apd.apd.payment_option (id, amount, description, due_date, fee, flow_reporting_id, receipt_id, inserted_date, is_partial_payment, iuv, last_updated_date, organization_fiscal_code, payment_date, payment_method, psp_company, reporting_date, retention_date, status, payment_position_id, notification_fee, last_updated_date_notification_fee, nav, fiscal_code, postal_code, province, region, type, validity_date, switch_to_expired) VALUES('${id}', 10000, '${description}', '2024-12-12 16:09:43.323', 0, NULL, NULL, '2024-11-12 16:09:43.477', false, '09455575462301733', '${formattedDate}', '${ecFiscalCode}', NULL, NULL, NULL, NULL, '2025-02-10 16:09:43.323', 'PO_UNPAID', ${paymentPositionId}, 0, NULL, '309455575462301733', '${debtorFiscalCode}', '89812', 'VV', 'CA', 'F', '2024-11-12 16:09:43.479', false)`);
}

async function updatePaymentPosition(id, pdStatus) {
    await connection.query(`UPDATE apd.apd.payment_position SET status='${pdStatus}' WHERE id='${id}'`);
}

async function updatePaymentOption(id, description) {
  await connection.query(`UPDATE apd.apd.payment_option SET description='${description}' WHERE id='${id}'`);
}

async function deletePaymentOption(id) {
  await connection.query(`DELETE FROM apd.apd.payment_option WHERE id='${id}'`);
}

async function insertTransfer(id, category, remittanceInformation, paymentOptionId) {
  await connection.query(`INSERT INTO apd.apd.transfer (id, amount, category, iban, transfer_id, inserted_date, iuv, last_updated_date, organization_fiscal_code, postal_iban, remittance_information, status, payment_option_id, hash_document, stamp_type, provincial_residence, company_name) VALUES('${id}', 10000, '${category}', 'mockIban', '1', '2024-11-12 16:09:43.477', '09455575462301733', '2024-11-12 16:09:43.477', '77777777777', NULL, '${remittanceInformation}', 'T_UNREPORTED', ${paymentOptionId}, NULL, NULL, NULL, 'SkyLab Inc.');`);
}

async function deleteTransfer(id) {
  await connection.query(`DELETE FROM apd.apd.transfer WHERE id='${id}'`);
}

function formatDate(date) {
  const pad = (n, z = 2) => ('00' + n).slice(-z);
  return date.getFullYear() + '-' +
    pad(date.getMonth() + 1) + '-' +
    pad(date.getDate()) + ' ' +
    pad(date.getHours()) + ':' +
    pad(date.getMinutes()) + ':' +
    pad(date.getSeconds()) + '.' +
    pad(date.getMilliseconds(), 3);
}

module.exports = {
  shutDownPool,
  insertPaymentPosition, deletePaymentPosition, deletePaymentPositionByIUPD, updatePaymentPosition,
  insertPaymentOption, updatePaymentOption, deletePaymentOption,
  insertTransfer, deleteTransfer
}