.. _project_plan:

.. note:: this section is under construction.

Project Plan
############


.. important:: This plan will be used to evaluate and manage the project. Key assumptions that affect the plan should be documented here. The project plan should be updated throughout the life-time of the project.



Summary of Project
******************

We will build a Distributed Revision Control System for geospatial datasets, written in Java.

For more information see the :ref:`proposal`.


Summary of Methodology
**********************

No formal development methodology has been defined. We'll approach development of the project following `agile <http://agilemanifesto.org/>`_ practices.

More or less fixed length iterations (about a month long), each of which will carry over a full development cycle, from requirements gathering to design, implementation and testing, will result in a deliverable with verifiable functionality.

Such functionalities will be extracted out of :ref:`user_stories` and will drive design decisions and acceptance tests.

We'll favor simplicity and pragmatism, and will require frequent input from participants.

We plan to use the following tools extensively through out the project:

* `Project website <http://opengeo.github.com/GeoGit/>`_
* `Project mailing list <https://groups.google.com/a/opengeo.org/group/geogit>`_
* `Issue tracking system <https://github.com/opengeo/GeoGit/issues>`_
* `Version control system <https://github.com/opengeo/GeoGit>`_
* `Automated build system <http://hudson.opengeo.org/hudson/view/geogit/>`_

This is an Open Source project, so collaboration is encouraged. Feel free to contact us at the mailing lists mentioned above to engage in the game.

How will releases be managed?
 * Releases will follow a `major.minor.bugfix[-MXX]` scheme. `M` stands for `Milestone` and `XX` for a milestone number;
 * A full development iteration closes a milestone;
 * Major (i.e. `1.0.0`) releases are not yet planned as we're just starting on the project;
 * Minor releases will be made after a certain number of Milestone releases
 * Bugfix releases will only contain bug fixes, no new features, and will only be considered when an important bug fix needs to be delivered over a minor release that's in production. So not for the foreseeable future.

How will changes be controlled?
 * Requests for requirements changes will be tracked in the issue tracker;
 * The change control board (CCB) will review requested changes and authorize work on them as appropriate;
 * After the feature complete milestone, no new features will be added to this release;
 * After the code complete milestone, no entirely new product source code will be added to this release;
 * All source code commit log messages must refer to a specific issue ID, after the feature complete milestone.

How will this plan be updated?
 This project plan will be updated as needed throughout the project. It will be placed under version control and instructions for accessing it will be on the project website. Any change to the plan will cause an automatic notification to be sent to a project mailing list.



.. _participants:

Project participants
********************

Roles and responsibilities
==========================

Customer Representative
 Member of the Customer community identified and made available to the project for their subject matter expertise. Its responsibility is to accurately represent their business units' needs to the Project Team, and to validate the deliverables that describe the product or service that the project will produce.

Customer Decision-Maker
 Member of the Customer community who has been designated to make project decisions on behalf of major business units that will use, or will be affected by, the product or service the project will deliver.

 `Customer Decision-Makers` are responsible for achieving consensus of their business unit on project issues and outputs, and communicating it to the `Project Manager`. They attend project meetings as requested by the `Project Manager`, review and approve process deliverables, and provide subject matter expertise to the `Project Team`.

 A `Customer Decision-Maker` may also serve as `Customer Representative`.

Project Manager
 The `Project Manager` is the person responsible for ensuring that the `Project Team` completes the project.

 The `Project Manager` develops the `Project Plan` with the team and manages the team’s performance of project tasks. It is also the responsibility of the `Project Manager` to secure acceptance and approval of deliverables from the project sponsors and stakeholders.

 The `Project Manager` is responsible for communication, including status reporting, risk management, escalation of issues that cannot be resolved in the team, and, in general, making sure the project is delivered in budget, on schedule, and within scope.

Project Team Member
 `Project Team Members` are responsible for executing tasks and producing deliverables as outlined in the `Project Plan` and directed by the `Project Manager`, at whatever level of effort or participation has been defined for them.

Technical Lead
 Member of the `Project Team` responsible for the underlying architecture for the software program, as well as for overseeing the work being done by any other software engineers working on the project. A `Technical Lead` will typically also act as a mentor for new or software developers or programmers, as well as for all the members of the `Project Team`.

 The `Technical Lead` also serves as technical advisor to management and provide programming perspective on requirements. Typically a `Technical Lead` will oversee a development team of between two and ten programmers, with three to five often considered the ideal size.

Software Developer
 `Project Team Member` concerned with facets of the software development process. Their work includes researching, designing, developing, and testing the software being built. A `Software Developer` may take part in various activities of the project life cycle (design, programming, testing). They may contribute to the overview of the project on the application level rather than component-level or individual programming tasks.

QA Lead
 Responsible to review Project Artifacts and work with the `Project Team` to define and create the overall test and QA strategy, and ensure that it is being achieved. Communicates the test and QA strategy to the `Project Manager` and `Development Team`, defines test processes including required test activities and deliverables. Identifies test resources, estimates test effort and defines test schedule and milestones. Ensures technical resources are organized for effective support of testing. Conducts walk-through of Test Strategy.

QA Analyst
 `Project Team` member that collaborates with the `QA Lead` on creation of the test strategy and on estimating testing effort. Its responsibilities incQlude reviewing requirements and logical and physical designs, collaborate with `QA Lead` to create the `QA Plan` and the `Test Plan`, participate in walk-through of `Test Plan`, create rest cases (manual and automated, if applicable).

Technical Writer
 Maintains technical documentation. This documentation includes online help, user guides/manuals, white papers, system manuals, test plans, etc.

Role Assignments
================

.. list-table:: 
   :widths: 20 20 20 40 
   :header-rows: 1 
   
   * - Role 
     - Person 
     - Status
     - Comments/Responsibilities
   * - Customer Representative
     - xxx
     - Pending
     - 
   * - Customer Decision-Maker
     - Rollie? Jeff? Justin?
     - Pending
     - 
   * - Project Manager 
     - Juan Marin
     - Assigned
     - 
   * - Technical Lead
     - Gabriel Roldan
     - Assigned
     - 
   * - QA Lead
     - Jeff Johnson?
     - Pending
     - 
   * - QA Analyst
     - Gabriel Roldan
     - Assigned
     - 
   * - QA Analyst
     - Syrus?
     - Pending
     - 
   * - Software Developer
     - Gabriel Roldan
     - Assigned
     - 
   * - Software Developer
     - Syrus?
     - Pending
     - 
   * - Software Developer
     - Mike?
     - Pending
     - 
   * - Technical Writer
     - Mike Pumphrey?
     - Pending
     - Write and maintain user facing documentation such as the :ref:`userguide`, faq, manual pages, etc. Draft 
       documents may be provided by other team members for his review and enhancement. 
   * - Beta Tester
     - Mike Pumphrey?
     - Pending
     - 
   * - Beta Tester
     - Jeff?
     - Pending
     - 
   
     
Use Cases Worksheet
===================

Column values:
 * *Frequency*: ``High|Medium|Low``. How frequently the use case is referred by user stories.
 * *Use Case status*: ``Name only|Initial|Base|Complete|Deferred``. How well defined the use case is.
 * *Implementation status*: ``None|Low|Medium|High``. Level of implementation for the use case.
 * *Difficulty*: ``Easy|Medium|Difficult``. Implementation difficulty.
 * *Priority*: ``Low|Medium|High``. Customer-Project Team agreed implementation priority.

.. table::

   ==================================  =========  ===============  ============ ============ ============ 
   Use Case                            Frequency  Use Case Status  Impl. Status Difficulty    Priority 
   ==================================  =========  ===============  ============ ============ ============ 
   :ref:`UC01`                         High        Base            Started       Easy        High
   :ref:`UC02`                         High        TBD             TBD           TBD         TBD
   :ref:`UC03`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC04`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC05`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC06`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC07`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC09`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC10`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC11`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC13`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC14`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC15`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC16`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC17`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC18`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC20`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC23`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC24`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC25`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC27`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC29`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC30`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC31`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC32`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC33`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC34`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC36`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC38`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC39`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC40`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC41`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC42`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC44`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC45`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC46`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC47`                         TBD         TBD             TBD           TBD         TBD
   :ref:`UC48`                         TBD         TBD             TBD           TBD         TBD
   ==================================  =========  ===============  ============ ============ ============ 

Deliverables in this Release
****************************



Risk Management
***************

.. toctree:: 
   :maxdepth: 2 
   
   risks

