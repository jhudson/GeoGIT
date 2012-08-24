.. use_case_suite:

Use Case Suite
##############

Common terms to all use cases:

* "User" is the use case's primary actor
* "System" is the system being built

* When referring to a GeoGit action as `command`, we mean a console terminal command (e.g. `$geogit status`), when referring to a GeoGit action as `operation`, we mean the execution of the action by the system, whether it's invoked from the command line or not.

.. note:: The use cases are deliberately phrased in a simple and pretty un-structure way.

.. _UC01:

UC01 Create Repository
**********************

.. list-table:: UC-01 Pull changes from remote
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - User creates and empty GeoGit repository using the command line interface.
   * - Direct actors
     - Owner
   * - Main success scenario
     -   1. Enter command ``geogit init [directory]`` and hit enter
         2. System creates an empty geogit repository in the given directory
         3. System displays an information message about the success of the operation
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - If no directory is given, the current working directory is assumed. If a geogit repository already exists in the target directory, and error message is displayed and the existing repository is unchanged. If the target directory has no geogit repository but is not empty? It may be a directory with shapefiles you want to keep close to the repo, so that'd be ok.

Back-traces:
 * :ref:`user_story_01`


.. _UC02:

UC02 Clone Repository
*********************

.. list-table:: UC-02 Clone Repository
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - Given a GeoGit repository, either local or web-published, and its URL, use the ``geogit clone`` operation to create an exact copy of it
   * - Direct actors
     - Contributor
   * - Main success scenario
     - 1. Enter the command ``geogit clone <URI>`` and hit enter. <URI> is the URL of the remote repository. Can be a file URL or a remote protocol URL (transport layer is probably HTTP, TBD)
       2. The system opens/connects to the remote repository and notifies the user
       3. The system starts fetching all needed objects and provides progress indication to the user
       4. The system finishes cloning the repository, which now contains an exact replica of the remote, and notifies the user.
   * - Alternative scenario extensions
     - 5. Provide a second parameter to the ``clone`` command with the name of the local repository to be created. The system will create a directory with such name and resume at step `2`.
       6. If an I/O error occurs during while connecting to the remote (e.g. network error or invalid URL), an error message is displayed and no local repository is created. Use case ends.
       7. If an I/O error occurs while fetching the remote repository contents (e.g. connect/read timeout), an error message is displayed and no local repository is created. Use case ends.
       8. After a connect/read timeout, User configures connection and read timeout using the ``geogit config`` command and resumes at step `1`.
   * - Notes and questions
     - 

Depends on:
 * :ref:`UC01`
 * :ref:`UC27`
 * :ref:`UC33`

Back-traces:
 * :ref:`user_story_02`
 * :ref:`user_story_05`
 * :ref:`user_story_07`
 * :ref:`user_story_03`


.. _UC03:

UC03 Pull changes from remote
*****************************

.. list-table:: UC-03 Pull changes from remote
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - Given a local repository with a configured remote, use the ``geogit pull <remote> <remote branch name>`` command 
       to fetch changesets from the specified remote branch and apply them to the current local branch.
   * - Direct actors
     - Contributor
   * - Main success scenario
     - 1. User enters command ``geogit pull <remote> <remote branch>`` and hits enter
       2. System connects to remote repository and grabs the remote branch reference
       3. System checks whether more information from the remote is needed
       4. System finds remote and local branches point to different commits
       5. System fetches and replies the changes from the remote branch since it diverged from the local branch, until 
          its current commit on top of master and record the result in a new commit along with the names of the two 
          parent commits and a log message from the user describing the changes.
       6. System presents a success message. Use case ends.
   * - Alternative scenario A:
     - 11. User has uncommitted changes on its local repository and calls ``pull <remote> <branch>``.
       12. At step 3, system finds one of the remote changes overlap with local uncommitted changes.
       13. System automatically cancels the merge and leaves the work tree untouched.
       14. System displays an error message. Use case ends.
   * - Notes and questions
     - * Both local and remote branches are assumed to be the `master` branch

Depends on:
 * :ref:`UC10`
 * :ref:`UC27`
 * :ref:`UC33`
 * :ref:`UC17`


Back-traces:
 * :ref:`user_story_02`
 * :ref:`user_story_04`


 .. _UC04:

UC04 Push changes to remote
***************************

.. list-table:: UC-04 Push changes to remote
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - Given a local repository with a configured remote, use the ``geogit push <remote> <remote branch name>`` to upload
       the local changesets not available on the remote repository and update the remote branch to point to the local
       branche's commit. 
   * - Direct actors
     - Collaborator
   * - Main success scenario
     - 1. User enters the ``geogit push <remote name> <remote branch>`` command and hits enter.
       2. System connects to remote repository and verifies its local information about the remote branch is up to date.
       3. System uploads the local changesets that are missing on the remote
       4. System instructs the remote to update its branch to point to the latest uploaded changeset
       5. System displays a success message, use case ends.
   * - Alternative scenario A:
     - 21. At point 2. System finds its local information about the remote branch is not up to date.
       22. System displays an informational message indicating to synchronize the remote information
       23. User runs the ``geogit pull <remote>`` command
       24. System updates the local repository with the missing changesets from remote, and finds no merge conflicts
       25. User re-runs the ``push`` command, use case resumes at `2.`
   * - Alternative scenario B:
     - 31. At point 2. System finds its local information about the remote branch is not up to date.
       32. System displays an informational message indicating to synchronize the remote information
       33. User runs the ``pull`` command with the ``--rebase`` option: ``geogit pull --rebase <remote> <branch>``
       34. System updates the local branch to match exactly the remote branch, and re-applies the local changes on top
       35. Use case resumes at `1.`
   * - Notes and questions
     - 

Depends on:
 * :ref:`UC03`
 * :ref:`UC10`
 * :ref:`UC33`
 * :ref:`UC41`

Back-traces:
 * :ref:`user_story_07`
 * :ref:`user_story_09`
 * :ref:`user_story_06`

.. _UC05:

UC05 Sparse-clone repository
****************************

.. list-table:: UC-05 Sparse-clone repository
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - A spatial filter is used as a ``geogit clone`` command argument to clone a repository
   * - Direct actors
     - Contributor
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 

.. _UC06:

UC06 Import FeatureType
***********************

.. list-table:: UC-06 Import FeatureType
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC07:

UC07 Export FeatureType
***********************

.. list-table:: UC-07 Export FeatureType
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC09:

UC08 Synchronize FeatureType
****************************

.. list-table:: UC-09 Synchronize FeatureType
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC10:

UC10 Commit Changes
*******************

.. list-table:: UC-10 Commit Changes
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC11:

UC11 Review Changeset
*********************

.. list-table:: UC-11 Review Changeset
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC13:

UC12 Create patch
*****************

.. list-table:: UC-13 Create patch
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC14:

UC14 Apply patch
****************

.. list-table:: UC-14 Apply patch
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC15:

UC15 Find differences
*********************

.. list-table:: UC-15 Find differences
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC16:

UC16 Manage branches
********************

.. list-table:: UC-16 Manage branches
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC17:

UC17 Merge branch
*****************

.. list-table:: UC-17 Merge branch
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC18:

UC18 Cherry-pick changesets
***************************

.. list-table:: UC-18 Cherry-pick changesets
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC20:

UC20 Switch branch
******************

.. list-table:: UC-20 Switch branch
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC23:

UC23 Configure ACL
******************

.. list-table:: UC-23 Configure ACL
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC24:

UC24 Revert changeset
*********************

.. list-table:: UC-24 Revert changeset
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC25:

UC25 List changesets
********************

.. list-table:: UC-25 List changesets
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC27:

UC27 Fetch objects from remote
******************************

.. list-table:: UC-27 Fetch objects from remote
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Contributor
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC29:

UC29 Read and modify with GeoTools
**********************************

.. list-table:: UC-29 Read and modify with GeoTools
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC30:

UC30 Configure GeoServer versioned FeatureTypes
***********************************************

.. list-table:: UC-30 Configure GeoServer versioned FeatureTypes
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC31:

UC31 Update through WFS
***********************

.. list-table:: UC-31 Update through WFS
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC32:

UC32 Configure Owner's Identity
*******************************

.. list-table:: UC-32 Configure Owner's Identity
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC33:

UC33 Manage Remotes
*******************

.. list-table:: UC-33 Manage Remotes
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Contributor
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC34:

UC34 Check modification status
******************************

.. list-table:: UC-34 Check modification status
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC36:

UC36 Reset changes
******************

.. list-table:: UC-36 Reset changes
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC38:

UC38 Web publish
****************

.. list-table:: UC-38 Web publish
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC39:

UC39 Filter changes
*******************

.. list-table:: UC-39 Filter changes
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC40:

UC40 Create tag
***************

.. list-table:: UC-40 Create tag
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC41:

UC41 Rebase
***********

.. list-table:: UC-41 Rebase
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 


.. _UC42:

UC42 Resolve conflicts
**********************

.. list-table:: UC-42 Resolve conflicts
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - Owner
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 

.. _UC44:

UC44 Sparse Clone
*****************

.. list-table:: UC-44 Sparse Clone
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - :ref:`Contributor`
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 

.. _UC45:

UC45 Shallow Clone
******************

.. list-table:: UC-45 Shallow Clone
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - :ref:`Contributor`
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     -
 
.. _UC46:

UC46 Support WMS with Time dimension
************************************

.. list-table:: UC-46 Support WMS with Time dimension
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - 
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 

.. _UC47:

UC47 Support WMS with "Branch" dimension
****************************************

.. list-table:: UC-47 Support WMS with "Branch" dimension
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - 
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 

.. _UC48:

UC48 Support WMS-C with "Time/Branch"
*************************************

.. list-table:: UC-48 Support WMS-C with "Time/Branch"
   :widths: 25 75
   :stub-columns: 1

   * - Summary
     - TBD
   * - Direct actors
     - 
   * - Main success scenario
     - TBD 
   * - Alternative scenario extensions
     - 
   * - Notes and questions
     - 

