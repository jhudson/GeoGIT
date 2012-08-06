.. _qa_plan:

QA Plan
#######

.. important:: This document specifies quality goals, selects strategies for assuring that those goals have been met, and details a plan of action to carry out those strategies.

.. note:: this section is under construction.

Introduction
************
"Quality" refers to all the good things that we would like to see in our product. We build a quality product and assure its quality by keeping quality in mind all the time and performing the selected activities below. Testing is one QA activity, but it is not the best or only one, other QA activities include the use of style guides and checklists, review meetings, use of analysis tools, and careful quality measurements and estimates. A plan is needed to select and coordinate all the QA activities.



QA Plan Scope
*************

There are many quality goals and approaches to assuring them. Since we have limited time and resources for this release, we will focus on the following components and aspects:

* :term:`CLI`
* Storage subsystem
* Basic command support..
* COMPONENT-X
* FEATURE-1
* FEATURE-2

.. todo:: TODO: Sum up the plan in a few sentences.


Summary
*******

In this release we will continue to use development practices that support all of our quality goals, but we will focus on functional correctness and robustness.

We will do that with the following major activities:

* using the facilities from the :term:`guava` library to test preconditions and to assert invariants and postconditions;
* conducting frequent reviews;
* performing automated unit and regression testing with :term:`JUnit`;
* carrying out structured manual :term:`system testing`;
* keeping all issues up-to-date in the project's :term:`issue tracker`.


Quality Goals for this Release
******************************

* Essential

  * :term:`Functionality > Correctness`
  * :term:`Functionality > Robustness`

* Expected

  * :term:`Functionality > Accuracy`
  * :term:`Maintainability > Understandability`
  * :term:`Maintainability > Evolvability`
  * :term:`Maintainability > Testability`

* Desired

  * :term:`Efficiency`

We explicitly will not focus on the following goals for this release, although we anticipate them to become important on future iterations:

* Deferred

  * :term:`Usability > Understandability and Readability`
  * :term:`Usability > Efficiency`
  * :term:`Usability > Task support`
  * :term:`Usability > Safety`
  * :term:`Usability > Consistency and Familiarity`
  * :term:`Usability > Subjective satisfaction`
  * :term:`Security`
  * :term:`Reliability > Consistency under load`
  * :term:`Reliability > Consistency under concurrency`
  * :term:`Reliability > Availability under load`
  * :term:`Reliability > Longevity`
  * :term:`Scalability`
  * :term:`Scalability > Performance under load`
  * :term:`Scalability > Large data volume`
  * :term:`Operability`


QA Strategy
***********

.. todo:: Consider the activities listed below and delete those that are not applicable to the project. Edit and add new activities if needed. For each activity, specify the coverage or frequency that you plan to achieve. If you do not plan to perform an activity, write "N/A".

.. list-table:: 
   :widths: 20 30 50
   :header-rows: 1

   * - Activity
     - Coverage or Frequency
     - Description
   * - Clear interface contracts
     - Every public interface
     - We will state clear contracts on all interfaces, including expected/allowed method input and output, preconditions, postconditions, and invariants. Will use `JSR 305 <http://jcp.org/en/jsr/detail?id=305>`_ annotations, but in the future may evaluate the possibility of using a `Design by contract <http://en.wikipedia.org/wiki/Design_by_contract>`_ library for Java if one of the available ones is deemed convenient. 
   * - Preconditions
     - All public methods that modify data
     - We will use :term:`GUAVA` precondition checking at the beginning of public methods to validate each argument value. This helps to document assumptions and catch invalid values before they can cause faults.
   * - Assertions
     - All private methods that modify data, or where it seems necessary
     - Since these methods are only called from our other methods, arguments passed to them should always be valid, unless our code is defective. Assertions will also be used to test class invariants and some postconditions.
   * - Static analysis
     - Automated detection of common errors
     - Static analisys tools like :term:`Findbugs` help detect common programming errors. 
   * - Code style checking
     - Required before every commit
     - An :term:`eclipse` code formatting configuration file is provided. Applying automatic code formatting is required for all java source code files. Peer review will make sure that commits that don't follow the coding conventions are rejected and the author is asked to redo the patch. This will help make all of our code consistent with our coding standards.
   * - Peer review
     - All changes to release branches
     - Whenever changes must be made to code on a release branch (e.g., to prepare a maintenance release) the change will be reviewed by another developer before it is committed. The goal is to help make sure that an obvious :term:`defect` is not introduced accidentally.
   * - Review meetings
     - **TBD** (weekly?)
     - We will hold review meetings where developers will perform formal inspections of selected code or documents. We choose to spend a small, predetermined amount of time and try to maximize the results by selecting review documents carefully. In the review process we will use and maintain a variety of checklists.
   * - Unit testing
     - All public methods that could possibly have a side effect, 90% - 100% code coverage.
     - We will consider the boundary conditions for each argument and test both sides of each boundary. Tests must be run and passed before each commit, and they will also be run by the testing team.
   * - Design for testability
     - Continuous
     - The system will be `designed for testability <http://en.wikipedia.org/wiki/Software_testability>`_ to the extent possible. At the micro-architectual level we'll make sure that individual classes are easily unit-testable with mocked up collaborators. At higher levels of abstractions we'll write proper integration tests.
   * - Manual system testing
     - 100% of specified requirements
     - The QA team will author and maintain a detailed written suite of manual tests to test the entire system through the user interface. This plan will be detailed enough that a person could repeatably carry out the tests from the test suite document and other associated documents. 
   * - Automated system testing
     - 100% of specified requirements
     - The QA team will use a system test automation tool to author and maintain a suite of test scripts to test the entire system through the user/programming interface. Ideally the library will support `scriptability <http://en.wiktionary.org/wiki/scriptability>`_ natively which will ease the writing of automatic system tests.
   * - Regression testing
     - Continuous
     - Developers run all unit tests before each commit. A continuous integration monitor will run all unit tests upon each commit.
   * - Load, stress, and capacity testing
     - Simple load testing and detailed analysis of each scalability parameter
     - We'll use a load testing tool and/or custom scripts to simulate heavy usage of the system. Load will be defined by scalability parameters such as number of concurrent users, number of transactions per second, or number/size of data items stored/processed. We will verify that the system can handle loads within its capacity without crashing, producing incorrect results, mixing up results for distinct users, or corrupting the data. We will verify that when capacity limits are exceeded, the system safely rejects, ignores, or defers requests that it cannot handle.
   * - Beta testing
     - **TBD** XX customers, XX members of the project team, outsiders?
     - We will involve outsiders in a beta test, or early access, program. We will release early and often, provide directions to focus on specific features of the system, and encourage them to report issues.


QA Strategy Evaluation
**********************
========================  =========  ============  ============  ===========  ===============  =======  ===========  ===========  ============  ========  ==========
  Goal                    Contracts  Static        Coding style  Peer review  Review meetings  Unit     Design for   System/reg.   Load/stress  Beta      Overall
                                     analysis                                                  testing  testability  testing       testing      testing   assurance
========================  =========  ============  ============  ===========  ===============  =======  ===========  ===========  ============  ========  ==========
Functionality                
 -- Correctness            High      High           Medium       Medium       Medium           High      High         Medium       Low          High       Strong
 -- Robustness             High      High           None         Medium       Medium           High      High         Medium       High         Medium     Strong
 -- Accuracy               Medium    None           None         Low          None             High      Low          High         None         Low        Acceptable
Maintainability            
 -- Understandability      High      Medium         Medium       High         Medium           High      High         High         Medium       None       Strong
 -- Evolvability           Low       None           Medium       High         Medium           Medium    High         Low          None         None       Acceptable
 -- Testability            High      None           Low          Medium       Low              High      High         High         High         High       Strong
Efficiency                 None      Low            High         Low          None             Low       None         Medium       High         High       Strong
========================  =========  ============  ============  ===========  ===============  =======  ===========  ===========  ============  ========  ==========

Cell values in the table above are subjective estimates of the effectiveness of each activity. This table helps to identify quality goals that are not being adequately assured.

Evaluation cell values:
 * High: This activity gives a strong assurance that the goal has been met in development.
 * Medium: This activity gives a medium assurance that the goal has been met in development.
 * Low: This activity gives only a little assurance that the goal has been met in development.
 * None: This activity does not address the goal.
 
Overall assurance values:
 * Strong: The set of activities together provide strong assurance that the goal has been met in development.
 * Acceptable: The activities together provide acceptable enough assurance that the goal has been met in development.
 * Weak: The activities together provide limited assurance that the goal has been met in development.
 * At-Risk: There is little or no assurance that this goal has been met.

.. note:: As a rule of thumb, it takes at least three "high" activities to give a "strong" raging, two "high" or one "high" and two "medium" to give an "acceptable" overall rating. Likewise, it takes at least two "medium" and one "low" activities to rate a "weak" overall rating.

Plan of Action
**************

.. note:: TODO: Once the plan is outlined, tasks should be assigned to individuals and tracked to completion.

#. Preconditions and Assertions
    * Refine requirements document whenever preconditions are not already determined
    * Create validation functions for use by preconditions and assertions, as needed
    * Write preconditions and assertions in code
#. Review meetings
    * Assign peer reviewers whenever a change to a release branch is considered
    * Select an at-risk document or section of code for weekly review meetings
    * Each week, identify reviewers and schedule review meetings
    * Reviewers study the material individually for 2 hours
    * Reviewers meet to inspect the material for 1 to 2 hours
    * Place review meeting notes in the repository and track any issues identified in review meetings
#. Unit tests
    * Set up infrastructure for easy execution of JUnit tests
    * Create unit tests for each class when the class is created
    * Execute unit tests before each commit. All tests must pass before developer can commit, otherwise open new issue(s) for failed tests. It's ok to start with "smoke tests" but don't keep it too dumb for too long.
    * Execute unit tests completely on each release candidate to check for regressions. These regression tests will be executed on a dedicated QA machine.
    * Update unit tests whenever requirements change
#. System tests
    * Design and specify a detailed manual test suite.
    * Review the system test suite to make sure that every UI screen and element is covered
    * Execute system tests completely on each release candidate. These system tests will be carried out on a dedicated QA machine.
    * Update system tests whenever requirements change
#. QA Management
    * Update this test plan whenever requirements change
    * Document test results and communicate them to the entire development team
    * Estimate remaining (not yet detected) defects based on current issue tracking data, defect rates, and metrics on code size and the impact of changes.
    * Keep all issues up-to-date in an issue tracking database. The issue tracker is available to all project members here. The meaning of issue states, priorities, and other attributes are defined in the SDM.


QA-Plan Checklist
*****************

Do the selected activities in the QA Strategy provide enough assurance that the project will meet it's quality goals?
  ..  Yes, if all activities are carried out as planned, we are confident that the quality goals will be satisfied. We will, of course, adjust this plan as needed.
  .. No, this plan leaves open several quality risks that have been noted in the Risk Management section of the Project Plan.
  Not yet, we're at the early stages of planning and the plan needs to be reviewed by different stakeholders.
  
Have human resources been allocated to carry out the QA activities?
  .. Yes, human resources have been allocated. They are listed in the Resource Needs document.
  .. No, human resources have not been allocated. They are listed as "pending" in the Resource Needs document.
  Not completely. They should be identified and listed as "pending" in the Resource Needs document.
  
Have machine and software resources been allocated as needed for the QA activities?
  Yes, the QA team will use desktop machines and servers that are already allocated to them. The QA/Integration server location and access mechanisms should be documented.
  .. Yes, a QA Lab has been set up. The needed machine and software resources are listed in the Resource Needs document.
  .. No, needed machine and software resources are listed as pending in the Resource Needs document.
  
Has this QA Plan been communicated to the development team and other stakeholders?
  .. Yes, everyone is aware of our prioritized quality goals for this release and understands how their work will help achieve those goals. Feedback is welcome.
  .. Yes, this document is being posted to the project website. Feedback is welcome.
  No, most stakeholders are not aware of and have not agreed upon the quality goals and planned QA activities for this release. This is a risk that is noted in the Risk Management section of the Project Plan.

