.. _proposal:

Project Proposal
################

.. important:: This proposal, along with drafts of related documents, will be used by management to determine whether or not to approve work on this project.

Background and Motivation
*************************
Versioning of geospatial datasets has a long history and different approaches and implementations by closed source vendors like `ArcSDE Versioning <http://geoserver.org/display/GEOS/Versioning+WFS+-+ArcSDE>`_ and `Oracle Workspace Manager <http://geoserver.org/display/GEOS/Versioning+WFS+-+Oracle+Worskspace+Manager>`_.

On the open source world there is OpenStreetMap's `history dumps <ttp://wiki.openstreetmap.org/wiki/User:MaZderMind/Reading_OSM_History_dumps>`_ and the Web Feature Service `Versioning extension <http://geoserver.org/display/GEOS/Versioning+WFS>`_.

Also, (as of August 2011) the OGC conducted an interoperability test bed on the usage of a new OpenGIS Web Service, `GeoSynchronization Service <http://www.opengeospatial.org/pub/www/ows8/geosync.html>`_, to enable the distributed synchronization of geospatial datasets through ATOM feeds containing WFS-T transaction elements that define the contents of change sets.

Back in September 2010, there were some `interesting discussions <http://osgeo-org.1560.n6.nabble.com/quot-git-quot-like-for-geodata-management-tc3847735.html>`_ on the OSGeo “discuss” mailing list regarding the existence and feasibility of a “git like” tool for geodata management, from which it's possible to conclude that nothing exists yet that easily enables distributed collaboration workflows for geo-data.

The question of whether something similar to “git” exists is timely because ``git`` and other `distributed version management systems <http://en.wikipedia.org/wiki/Distributed_revision_control>`_ arose in the last years making a big impact in how source code is managed, and it becomes natural to ask if these benefits could be achieved for the geospatial data domain. The first instinctive reaction is to question whether such an existing tool could be used directly to manage geospatial data, and a quick assessment of the technology makes it clear that though “possible” it would not be practical because of three fundamental differences in the problem domain:

* **Data volume**: source code management systems won't scale well for the sheer size of information that a geospatial dataset may encompass. `SCM <http://en.wikipedia.org/wiki/Source_Control_Management>`_'s work with plain files on disk and scale well up to a couple dozen thousand files, then their performance degrade too much due to a variety of reasons, most notably the stress imposed to the disk caches for “working tree” lookups and memory constraints.
* **Data representation**: source management systems are intrinsically prepared to work with text files as the unit of work, and so the algorithms to identify differences between two versions of the same file. To do so, ``git`` works with canonical internal representations of each text file, so that it can compare any two versions of the same file by merely comparing the `SHA1 <http://en.wikipedia.org/wiki/SHA-1>`_ hash of their *contents*. Geospatial data is best represented (at least for storage and processing) in binary form and the unit of work would be a *Feature* instead of a text file, and hence the geo-data version management system should work with a binary internal representation of a Feature that allows for easy diff'ing between two versions of the same Feature.
* **Data distribution**: source code is usually organized in directories forming a more or less deep tree of directories and files, producing small internal representations of each tree. Geo-data, on the contrary, is organized in very flat namespaces with potentially lots of items for a single data set or “leaf tree node”, making it impractical to hold such a large tree representation in memory at once.

There is also another fundamental difference between how source code management tools and existing geo-data management approaches work, that would be worth considering and trying to solve for the geospatial domain: the task of **managing source code is totally orthogonal to the task of working on the source code**, allowing developers to chose the best tool for the job, like in using emacs or eclipse as coding environments, regardless of if the version history is tracked by svn, git, CVS, etc. Conversely, all geo-data versioning systems so far are tightly coupled to the storage mechanisms and the tools to work with the data, generally forcing the use of a specific database and edit tools, imposing a workflow, and even the modification of the original data structures to account for extra information needed by the versioning system, further complicating the proliferation of a collaboration tool outside very constrained environments.

Finally, what's compelling about following the ``git`` principles and basic design to build a geospatial distributed version control system, is that the basics of ``git``'s architecture are really simple yet, very powerful. The immutability of the objects and the separation of concerns between an object's contents and its metadata (name, location, etc) makes the object model really suitable for a wide variety of target platforms, from handheld devices to could servers.

Goal
****

The project will produce a distributed geo-data versioning system that enables different collaboration workflows without imposing the technology restrictions that current approaches, proprietary and open source, encompass.

The following are the defining features and benefits of this product.

Some of them directly taken, and adapted where applicable, from the `“Why git is better than X” <http://whygitisbetterthanx.com/>`_ site. Full credit to the original authors is given.

Cheap Local Branching
=====================


By following the GIT branching model, GeoGIT “will allow to have multiple local branches that can be entirely independent of each other and the creation, merging and deletion of those lines of development take seconds.” This means that you can do things like:

* Create a branch to try out an idea, commit a few times, switch back to where you branched from, apply a patch, switch back to where you are experimenting, then merge it in.
  * Have a branch that always contains only what goes to production, another that you merge work into for testing and several smaller ones for day to day work
* Create new branches for each new feature you're working on, so you can seamlessly switch back and forth between them, then delete each branch when that feature gets merged into your main line.
* Create a branch to experiment in, realize it's not going to work and just delete it, abandoning the work—with nobody else ever seeing it (even if you've pushed other branches in the meantime) Importantly, when you push to a remote repository, you do not have to push all of your branches. You can only share one of your branches and not all of them. This tends to free people to try new ideas without worrying about having to plan how and when they are going to merge it in or share it with others.

Distributed
===========

This means that even if you're using a centralized workflow, every user has what is essentially a full backup of the main server, each of which could be pushed up to replace the main server in the event of a crash or corruption. There is basically no single point of failure with Git unless there is only a single point.



.. toctree::
   :maxdepth: 2

   audience_benefits
