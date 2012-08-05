.. _product:

GeoGit
########

.. epigraph::  “Every (Geo)Git clone is a full-fledged repository with complete history and full revision tracking capabilities, not dependent on network access or a central server. Branching and merging are fast and easy to do.”

  -- http://git-scm.com/

GeoGIT is a distributed version control system for geographic information, written in Java and inspired by the `GIT <http://git-scm.com/>`_ distributed version control system design and principles, adapting them to the differences in the problem domain, but having absolutely no relation with the GIT project.


Mission and Scope
*****************
For background reading please review this GeoServer wiki page: `GeoGit approach <http://geoserver.org/display/GEOS/GeoGit+approach>`_.


Problem
=======
This project aims to address the need of having a **distributed version control system** for vector geospatial datasets - although supporting raster datasets version is not discarded for the future -.

The project name and much of its intended features are strongly inspired by the GIT distributed version control system, but tailored towards working with binary geospatial data as opposed to source code or text files.

Goal
====
To develop a Java library and a set of utilities that enable the creation of and interaction with repositories of geospatial datasets that contain full dataset version history, that are distributed (and hence disconnected) in nature, that allows for easy and `cheap` creation of `branches of development`, allows to query and merge changes between branches, and permits almost any collaboration workflow by pulling and pushing changes from and to `remote` repositories.

Additionally, the project should:

* Handle small to very large projects Enable version control for a diverse range of geospatial data back ends;
* Serve as the basis for a variety of versioning, data interchange and synchronization protocols (e.g. WFS2 Versioning and OGC's GeoSynchronization Service);
* Enable multiple collaboration workflows on different computing platforms and environments (desktop, server, Andorid handheld devices).
* Treat geodata versioning as an orthogonal problem to data editing, allowing to chose the best tool for each job instead of imposing any given set of highly coupled software solutions.

Scope
=====
We want to focus on the development of the low level library, and a minimal set of utilities around it that are user faced enough as to enable the integration with other systems, such as web based front ends, versioning data stores, command line utilities, and export and import of repository snapshots to common data formats.


Where should a new team member start?
=====================================
For more information, see the :ref:`proposal`.

When you're done, check the :ref:`sourceandbuild`.

Status
******

We are at the very early stages of developement, planning for the next iteration, gathering user stories and defining scope and overall system design.

But it looks we're standing on pretty firm floor. The previous round of development lead to a `working prototype <http://repo.opengeo.org/org/geogit/geogit/1.0-M1/>`_, which is functional enough as to provide the basis for OGC's WFS-2 versioning needs and OGC's GeoSynchronization Service replication back end, living on the OpenGeo `geosync GeoServer branch <https://github.com/opengeo/geoserver/tree/geosync>`_.

This first Milestone has been achieved with basic support for ``add``, ``commit``, ``diff``, and ``log`` operations. Most aspects of how GIT abstractions map to the geospatial domain have been studied and some have already being addressed, and others are conceptually defined.

Project Documents
#################

.. toctree::
   :maxdepth: 2

   project_planning/index
   requirements/index
   architecture/index
   implementation/index
   deployment/index
   operation_and_support/index
   
