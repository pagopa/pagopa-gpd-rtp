@clean-up-required
Feature: All about RTP events

  Background:
    Given an EC with fiscal code '77777777777' and flag opt in enabled on Redis cache

  @clean-up-required
  Scenario Outline: Checking for excluded debt position not sent to RTP
    Given a create a 'VALID' payment position with id prefix '123121' and fiscal code '77777777777' on GPD database
    And a create payment option with id prefix '123122', description 'description Mario Rossi' and associated to the previous payment position on GPD database
    And a create transfer with id prefix '123123', category '<taxonomy>', remittance information of primary ec '/RFB/091814449948492/547.24/TXT/DEBITORE/VNTMHL76M09H501D' and associated to the previous payment option on GPD database
    When in the RTP topic does not exist a 'create' operation with id suffix 'c' in 10000 ms
    Then the 'create' RTP message has not been sent through event hub
    Examples:
      | taxonomy |
      | 6/0101100IM/ |
      | 7/0101100IM/ |
      | 8/0101100IM/ |
#
#  @clean-up-required
#  Scenario Outline: Checking for excluded debt position not sent to RTP with wrong payment position status
#    Given a create a '<debtPositionStatus>' payment position with id prefix '123121' and fiscal code '77777777777' on GPD database
#    And a create payment option with id prefix '123122', description 'description Mario Rossi' and associated to the previous payment position on GPD database
#    And a create transfer with id prefix '123123', category '<taxonomy>', remittance information of primary ec '/RFB/091814449948492/547.24/TXT/DEBITORE/VNTMHL76M09H501D' and associated to the previous payment option on GPD database
#    When in the RTP topic does not exist a 'create' operation with id suffix 'c' in 10000 ms
#    Then the 'create' RTP message has not been sent through event hub
#    Examples:
#      | debtPositionStatus |
#      | DRAFT |
#      | PUBLISHED |
#      | REPORTED |
#
#  @clean-up-required
#  Scenario Outline: Checking for the taxonomy formally not correct and sent to RTP
#    Given a create a 'VALID' payment position with id prefix '123121' and fiscal code '77777777777' on GPD database
#    And a create payment option with id prefix '123122', description 'description Mario Rossi' and associated to the previous payment position on GPD database
#    And a create transfer with id prefix '123123', category '<taxonomy>', remittance information of primary ec '/RFB/091814449948492/547.24/TXT/DEBITORE/VNTMHL76M09H501D' and associated to the previous payment option on GPD database
#    When in the RTP topic exists a 'create' operation with id suffix 'c' in 20000 ms
#    Then the 'create' operation has the first transfer's remittance information
#    And the first transfer's remittance information of 'create' operation does not contain 'VNTMHL76M09H501D'
#    And the 'create' RTP message has the anonymized description 'description M**** R****'
#    And the create operation has the status 'VALID'
#    Examples:
#      | taxonomy |
#      | 6/Pippo |
#      | 7/Pippo |
#      | 8/Pippo |
#      | 9/Pippo |
#      | Pippo |
#
#  @clean-up-required
#  Scenario: Checking for the taxonomy formally correct and sent to RTP
#    Given a create a 'VALID' payment position with id prefix '123121' and fiscal code '77777777777' on GPD database
#    And a create payment option with id prefix '123122', description 'description Mario Rossi' and associated to the previous payment position on GPD database
#    And a create transfer with id prefix '123123', category '9/0101100IM/', remittance information of primary ec '/RFB/091814449948492/547.24/TXT/DEBITORE/VNTMHL76M09H501D' and associated to the previous payment option on GPD database
#    When in the RTP topic exists a 'create' operation with id suffix 'c' in 20000 ms
#    Then the 'create' operation has the first transfer's remittance information
#    And the 'create' RTP message has the anonymized description 'description M**** R****'
#    And the create operation has the status 'VALID'
#
#  @create-payment-position-required
#  @clean-up-required
#  Scenario: Update payment option in GPD database is published into RTP event hub
#    Given an update operation on field description with new value 'description updated Mario Rossi' on the same payment option in GPD database
#    When in the RTP topic exists a 'update' operation with id suffix 'u' in 20000 ms
#    Then the 'update' operation has the first transfer's remittance information
#    And the update operation has the description 'description updated Mario Rossi'
#    And the 'update' RTP message has the anonymized description 'description updated M**** R****'
#
#  @create-payment-position-required
#  @clean-up-required
#  Scenario: Update payment option in GPD database is published into RTP event hub
#    Given an update operation on field status with new value 'REPORTED' on the same payment position in GPD database
#    And an update operation on field description with new value 'description updated Mario Rossi' on the same payment option in GPD database
#    When in the RTP topic does not exist a 'update' operation with id suffix 'u' in 10000 ms
#    Then the 'update' RTP message has not been sent through event hub
#
#
#  @create-payment-position-required
#  @clean-up-required
#  Scenario: Delete payment option in GPD database is published into RTP event hub
#    Given a delete operation on the same transfer in GPD database
#    And a delete operation on the same payment option in GPD database
#    When in the RTP topic exists a 'delete' operation with id suffix 'd' in 20000 ms
#    Then the RTP topic returns the 'delete' operation with id suffix 'd'
#
#
#  @clean-up-required
#  Scenario Outline: Checking for a foreign debtor (natural and legal person)
#    Given a create a 'VALID' payment position with id prefix '123121' and fiscal code '77777777777' on GPD database
#    And a create payment option with id prefix '123122', fiscal code '<debtorFiscalCode>', description 'description <name>' and associated to the previous payment position on GPD database
#    And a create transfer with id prefix '123123', category '9/0101100IM/', remittance information of primary ec '/RFB/091814449948492/547.24/TXT/DEBITORE/<debtorFiscalCode>' and associated to the previous payment option on GPD database
#    When in the RTP topic exists a 'create' operation with id suffix 'c' in 20000 ms
#    Then the 'create' operation has the first transfer's remittance information
##    And the first transfer's remittance information of 'create' operation does not contain '<debtorFiscalCode>' # under analysis
#    And the create operation has the status 'VALID'
#    Examples:
#      | debtorFiscalCode | name |
#      | XZ-9042715-Q  | Mark Red |
#      | QQ 12 34 56 A | Mark Lenders |
#      | 12345678901   | Company Inc. |
#      | DE 260543999  | Nilla Pizzi |