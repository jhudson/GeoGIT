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
    
