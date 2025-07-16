Feature: All about RTP events

  Background:
    Given an EC with fiscal code '77777777777' and flag opt in enabled on Redis cache
    And a create payment position with id prefix '123121' and fiscal code '77777777777' on GPD database
    And a create payment option with id prefix '123122', description 'description Mario Rossi' and associated to the previous payment position on GPD database
    And a create transfer with id prefix '123123', category '9/0201102IM/', remittance information '/RFB/091814449948492/547.24/TXT/DEBITORE/VNTMHL76M09H501D' and associated to the previous payment option on GPD database

  Scenario: New payment option in GPD database is published into RTP event hub
    When the operations have been properly published on RTP event hub after 20000 ms
    Then the RTP topic returns the 'create' operation with id suffix 'c'
    And the 'create' operation has the remittance information
    And the 'create' RTP message has the anonymized description 'description M**** R****'
    And the create operation has the status 'VALID'

  Scenario: Update payment option in GPD database is published into RTP event hub
    Given an update operation on field description with new value 'description updated Mario Rossi' on the same payment option in GPD database
    When the operations have been properly published on RTP event hub after 20000 ms
    Then the RTP topic returns the 'update' operation with id suffix 'u'
    And the 'update' operation has the remittance information
    And the update operation has the description 'description updated Mario Rossi'
    And the 'update' RTP message has the anonymized description 'description updated M**** R****'

  Scenario: Delete payment option in GPD database is published into RTP event hub
    Given the create operation has been properly published on RTP event hub after 20000 ms
    And a delete operation on the same transfer in GPD database
    And a delete operation on the same payment option in GPD database
    When the operations have been properly published on RTP event hub after 20000 ms
    Then the RTP topic returns the 'delete' operation with id suffix 'd'