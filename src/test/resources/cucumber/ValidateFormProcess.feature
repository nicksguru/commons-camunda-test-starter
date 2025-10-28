@bpmn #@disabled
Feature: BPMN process 'ValidateForm'

  Background:
    Given load process definition "ValidateForm" from file "bpmn/ValidateForm.bpmn"

  # process remains active after this test (which is important for message correlation)
  Scenario: Process state right after start
    Given process business key is "orderId:1"
    When start process
    Then process does not end
    And process awaits at activity "InputFormMessageWaitEvent"
    And delegate validateFormDelegate was not called
    And process variable "orderId" is null

  # process remains active after this test (which is important for message correlation)
  Scenario: Send message to process to user task
    Given process business key is "orderId:2"
    When start process with variables:
      | orderId | 2 |
    And send process message "InputFormMessage"
    Then process does not end
    And process awaits at activity "UserTask_InputValue"
    And delegate validateFormDelegate was not called
    And process variable "orderId" is "2"

  Scenario: User task - a number is entered
    Given process business key is "orderId:3"
    When start process with variables:
      | orderId | 3 |
    And send process message "InputFormMessage"
    Then process does not end
    And process awaits at activity "UserTask_InputValue"
    When user fills in form:
      | orderTotal | 1.23 |
    # pause occurs because this step has async-before and, with jobs disabled, manual advancement is required
    Then process awaits at activity "ServiceTask_ValidateForm"
    Then delegate validateFormDelegate was called
    And process variable "isFormValid" is "true"
    And process variable "orderId" is "3"
    # no paused because further steps have no async-before
    And process ends
    And process visited activities:
      | Gateway_IsFormValid           |
      | MainSubprocessEnd_FormIsValid |
      | ProcessEnd                    |

  Scenario: User task - not a number is entered
    Given process business key is "orderId:4"
    When start process with variables:
      | orderId | 4 |
    And send process message "InputFormMessage"
    Then process does not end
    And process awaits at activity "UserTask_InputValue"
    When user fills in form:
      | orderTotal | not a number |
    # pause occurs because this step has async-before and, with jobs disabled, manual advancement is required
    Then process awaits at activity "ServiceTask_ValidateForm"
    Then delegate validateFormDelegate was called
    And process variable "isFormValid" is "false"
    And process variable "orderId" is "4"
    But process does not end
    And process awaits at activity "UserTask_InputValue"
    And process visited activities:
      | Gateway_IsFormValid                    |
      | MainSubprocessEscalation_FormIsInvalid |
      | MainSubprocessStart                    |

  Scenario: User task - empty value is entered
    Given process business key is "orderId:5"
    When start process with variables:
      | orderId | 5 |
    And send process message "InputFormMessage"
    Then process does not end
    And process awaits at activity "UserTask_InputValue"
    # input empty value
    When user fills in form:
      | orderTotal |  |
    # pause occurs because this step has async-before and, with jobs disabled, manual advancement is required
    Then process awaits at activity "ServiceTask_ValidateForm"
    Then delegate validateFormDelegate was called
    And process variable "isFormValid" is "false"
    And process variable "orderId" is "5"
    # retry because of escalation
    But process does not end
    And process awaits at activity "UserTask_InputValue"
    # check that retry has not altered any variables
    And process variable "orderId" is "5"
    And process visited activities:
      | Gateway_IsFormValid                    |
      | MainSubprocessEscalation_FormIsInvalid |
      | MainSubprocessStart                    |
