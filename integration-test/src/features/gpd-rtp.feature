Feature: All about RTP events

  Background:
    Given an EC with fiscal code '77777777777' and flag opt in enabled on Redis cache

  Scenario: New payment option in GPD database is published into RTP event hub
    Given a create payment position with id '123123121' and fiscal code '77777777777' on GPD database
    And a create payment option with id '123123122' and associated to payment position with id '123123121' on GPD database
    And a create transfer with id '123123123', category '9/0201102IM/', remittance information '/RFB/091814449948492/547.24/TXT/DEBITORE/VNTMHL76M09H501D' and associated to payment option with id '123123122' on GPD database
    When the operations have been properly published on RTP event hub after 20000 ms
    Then the RTP topic returns the 'create' operation with id '123123122-c'
    And the create operation has the remittance information anonymized
    And the create operation has the status 'VALID'

  Scenario: Update payment option in GPD database is published into RTP event hub
    Given a payment option with id '123123122' in GPD database
    And an update operation on field status with new value 'PAID' on the same payment position in GPD database
    When the operations have been properly published on RTP event hub after 20000 ms
    And the RTP topic returns the 'update' operation with id '123123122-u'
    And the update operation has the status 'PAID'

  Scenario: Delete payment option in GPD database is published into RTP event hub
    Given a payment option with id '123123122' in GPD database
    And a transfer with id '123123123' in GPD database
    Given a delete operation on the same transfer in GPD database
    And a delete operation on the same payment option in GPD database
    When the operations have been properly published on RTP event hub after 20000 ms
    And the RTP topic returns the 'delete' operation with id '123123122-d'