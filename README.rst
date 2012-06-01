######################################################
GeoGit - Geospatial Distributed Version Control System
######################################################

Welcome to the GeoGit project, exploring the use of distributed management of spatial
data.

For background reading please review this GeoServer wiki page: `GeoGit approvah`:http://geoserver.org/display/GEOS/GeoGit+approach

Details
=======

*Project Lead*

`Gabriel Roldan`:https://github.com/groldan

*Header*

Source files use the following header::
   
   /* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
    * This code is licensed under the LGPL 2.1 license, available at the root
    * application directory.
    */
 
As indicated above the code is distributed under an `LGPL 2.1`:LICENSE.txt license.

*Build*

GeoGit is built using Maven::
  
  mvn clean install

Additional build profiles are documented in the root `pom.xml`:pom.xml ::
  
  more pom.xml

Participation
=============

The project is hosted on github:

* https://github.com/opengeo/GeoGit

Participation is encouraged using the github *fork* and *pull request* workflow::

* file headers are described above
* include test case demonstrating functionality
* contributions are expected to pass test and not break the build

Additional resources:

* `GeoGit Group`:https://groups.google.com/a/opengeo.org/group/geogit/
* The build is `actively monitored using hudson`:http://hudson.opengeo.org/hudson/view/geogit/
* https://github.com/opengeo/GeoGit/issues