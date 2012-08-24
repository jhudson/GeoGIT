.. _sourceandbuild: 

Source and Build Instructions
#############################

Welcome to the GeoGit project, exploring the use of distributed management of spatial data.

Participation
*************

The project is hosted on github: https://github.com/opengeo/GeoGit

Participation is encouraged using the github fork and pull request workflow.

Include test cases demonstrating functionality, contributions are expected to pass tests and not break the build.

Project resources
*****************

Join the `GeoGit Group <https://groups.google.com/a/opengeo.org/group/geogit/>`_ and talk to us early. Do not take the risk of your contribution being rejected or needing major modifications because you didn't engage in the community and its rules early in your development cycle.

Issue tracking
**************

Bugs and improvement requests are tracked at the project's GitHub `issue tracker <https://github.com/opengeo/GeoGit/issues>`_. However, please speak to the mailing list first before filing bug reports just because. Core developers will be able to confirm a bug and ask you to file a bug report, or direct you to the solution if there's one already.

Licence Headers
***************

All `.java` files shall have the following header as the top-most content:


::


   /* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
    * This code is licensed under the LGPL 2.1 license, available at the root
    * application directory. 
    */

As indicated above the code is distributed under an LGPL 2.1 license.

Build
*****

GeoGit is built using `Maven <http://maven.apache.org/>`_::
    
    $cd geogit/src
    $mvn clean install

Online tests, require a geogit endpoint, are available using:: 

   mvn -Ponline

Test coverage and continuous integration
****************************************

The `Cobertura Maven plugin <http://maven-plugins.sourceforge.net/maven-cobertura-plugin/>`_ is configured for a test coverage report::

    $mvn cobertura:cobertura
    $open target/site/cobertura/index.html 
    
Any additional build profiles are documented in the root `pom.xml`.

The build is actively monitored using `hudson <http://hudson.opengeo.org/hudson/view/geogit/>`_.

Code formatting
***************

Please carefully apply the code formatting options in the `buld/eclipse/formatter.xml` file. These are the standard Java formatting options with 100 character line length for both code and comments, and 4 spaces for indentation. It is also recommended to use the code templates from `build/eclipse/codetemlates.xml`.

Additional resources
********************

* `guava-libraries <http://code.google.com/p/guava-libraries/>`_
* `Berkeley DB Java Edition <http://www.oracle.com/technetwork/database/berkeleydb/overview/index-093405.html>`_
