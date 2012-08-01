######################################################
GeoGit - Geospatial Distributed Version Control System
######################################################

Welcome to the GeoGit project, exploring the use of distributed management of spatial
data.

For background reading please review this GeoServer wiki page: `GeoGit approach <http://geoserver.org/display/GEOS/GeoGit+approach>`_

Details
=======

Project Lead: `Gabriel Roldan <https://github.com/groldan>`_

Source files use the following header::
   
   /* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
    * This code is licensed under the LGPL 2.1 license, available at the root
    * application directory.
    */
 
As indicated above the code is distributed under an `LGPL 2.1 <LICENSE.txt>`_ license.

Build
-----

GeoGit is built using Maven::
  
  mvn clean install

Online tests, require a geogit endpoint, are available using::

  mvn -Ponline

Cobertura is configured for a test coverage report::

  mvn cobertura:cobertura
  open target/site/cobertura/index.html
    
Any additional build profiles are documented in the root `pom.xml`:pom.xml .

If you would like to work in Eclipse use of the `m2eclipse plugin <http://www.sonatype.org/m2eclipse>`_ recommended.

Please carefully apply the code formatting options in the buld/eclipse/formatter.xml file. These are the standard
Java formatting options with 100 character line length for both code and comments, and 4 spaces for indentation.
It is also recommended to use the code templates from build/eclipse/codetemlates.xml.

Participation
=============

The project is hosted on github:

* https://github.com/opengeo/GeoGit

Participation is encouraged using the github *fork* and *pull request* workflow::

* file headers are described above
* include test case demonstrating functionality
* contributions are expected to pass test and not break the build

Project resources:

* `GeoGit Group <https://groups.google.com/a/opengeo.org/group/geogit/>`_
* The build is `actively monitored using hudson <http://hudson.opengeo.org/hudson/view/geogit/>`_
* https://github.com/opengeo/GeoGit/issues

Additional resources:

* `guava-libraries <http://code.google.com/p/guava-libraries/>`_

