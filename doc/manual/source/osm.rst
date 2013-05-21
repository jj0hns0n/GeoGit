Using GeoGit with OpenStreetMap data
=====================================

GeoGit can be used to version OSM data, and also to contribute changes back once the work is done. GeoGit aims to provide all the tools needed for the versioning part of a normal OSM editing workkflow, adding some of its powerful tools to give additional possibilites.

This section describes the GeoGit commands that interact with OSM and their usage.

Importing OSM data.
--------------------

Just like you can import data form a shapefile or a PostGIS database into a GeoGit repository, you can also import OSM data. The ``osm import`` is the one to use for that.

There are two ways of importing OSM data: from a file or connecting to a OSM endpoint that supports the OSM Overpass API.

If you have a file, just use the ``osm import`` command with the following syntax:

::

	geogit osm import <path_to_file>

Both OSM XML and pbf formats are supported.

If you want to download the data from a OSM server instead, additional parameters are needed. The syntax in that case is as follows:

::

	geogit som import [<server_URL>] [--filter <filter_file>] [--bbox <S> <W> <N> <E>]

You can specify the server from which you want to get your OSM data, just entering its URL after the ``osm import`` command. By default, if no URL is provided, the import command uses ``http://overpass-api.de/api/interpreter`` as endpoint. 

To avoid downloading the full OSM planet, a filter can be used. You should write your filter in a separate text file using the Overpass Query Language. Check the `language documentation <http://wiki.openstreetmap.org/wiki/Overpass_API/Language_Guide>`_ to know more about it.

A few considerations to take into account:

- Version information are needed to parse the downloaded data. Use the verbose mode (``out meta;``) to get the version information added.

- If your filter download ways, it should also download the corresponding nodes. For instance, this filter will add no data to your GeoGit repo:

	::

		way
			["name"="Gielgenstraße"]
			(50.7,7.1,50.8,7.25);
		out meta;

	The following one, however, will work:

	::

		(
		  way
		    ["name"="Gielgenstraße"]
		    (50.7,7.1,50.8,7.25);
		  >;
		);
		out meta;


If the filter you want to set is just a bounding box filter, you can use the ``--bbox`` option as a practical alternative, as in the next example:

::

	$ geogit osm import --bbox 50.7 7.1 50.8 7.25

Values after the ``--bbox`` option represent South, West, North and East limits, respectively.

Downloaded data is imported into GeoGit and put into two trees: ``way`` and ``node`` with a very basic feature type in both cases . If you want to structure your data differently, you can specify a mapping. This will cause the feature types to be different, created according to the mapping and using the entity tags to fill the corresponding attribute values.

Nodes and ways will still be placed in the ``node`` and ``way`` trees, and the mapping cannot change that. This is particularly importante in the case of nodes, because ways are expressed as a set of nodes, so GeoGit must be able to find the corresponding nodes and retrieve their coordinates. This is done by keeping them always in the ``node`` tree and using the node Id as te feature Id of the feature representing the node.

Apart from the attributes defined in the mapping, GeoGit always keeps the ``tags``, ``nodes`` and ``user`` attributes, which are used if the original data has to be later re-created, for instance, when exporting it.

[Mappings are stil not available. When they are implemented, they should be described here]


Unlike the case of importing from a shapefile or database, importing from OSM has to be performed with a clean index and working tree, and the imported data is not just imported into the workign tree, but also staged and commited. This is done to ensure that the commit actually correspond to an OSM changeset, with no further modification, so it can be later identified and used as a reference when performing other tasks agains the OSM planet, such as updating.

Updating
---------

If you have imported OSM data into your GeoGit repository, you can easily update it to get the new changes that might have been added in the central OSM planet. To do so, just run the ``osm update`` command.

::

	$ geogit osm update

As in the case of importing, you can select a URL different to the default one, just entering it after the command.

::

	$ geogit osm update http://overpass.osm.rambler.ru/

The filter that you used for the latest import will be used, and also the mapping, if any. In case you want to get the most recent OSM data with a different filter, you should run the ``import`` command instead, which will replace the current OSM data in the geogit repository.

The ``update`` command is similar to the ``pull`` command in a normal repository. It will get the latest version of the OSM data and put it in new temporary different branch. That branch starts at the commit where you made your last update. From that point GeoGit will try to merge that branch with your current branch, doing it the usual way. If you have edited your OSM and your changes are not compatible with the changes introduced in the latest snapshot that you you have just downloaded, conflicts will be signaled, and you should resolve them.

As in the case of the ``pull`` command, you can tell GeoGit to perform a rebase instead of a merge, by using the ``--rebase`` switch.

Exporting
----------

The content of a geogit repository can be exported in OSM XML format, much in the same way as it owrks for other formats such as shapefiles. The OSM export command has the following format:

::

	geogit osm export

The OSM format is to be used as a part of a normal OSM workflow, both for importing and exporting. If you plan to edit your data and create new versions in your geogit repo that you can later contribute back to the OSM planet, the OSM XML format has to be used. Other formats will not guarantee that the relation between nodes and ways is kept, and the result of a workflow might result in a new snapshot in the geogit repository that cannot be later exported and contributed back to the OSM planet.

The geometry of ways is not used to export, and is assumed to match the set of nodes that is kept in the ``nodes`` attribute. That's the reason why the OSM format should be used instead of other formats when exporting OSM data. Using other format can lead to unconsistent relations between nodes and ways.

Even if a mapping was defined when importing, GeoGit will restore the original structure of the OSM XML file back, so mappping do not affect the export process.

The area to export can be restricted by using the ``--b`` option, which works just as it does in the case of the ``import`` command. Use it to define a bounding box, and only those elements intersecting the selected area will be exported.


Exporting differences as changesets
------------------------------------

The differences between two commits in a repository can be exported as an OSM changeset that can be used to contribute those changes to the OSM planet. To export differences as changesets, the following command has to be used:

::

	geogit osm create-changesets [commit [commit]] -f <changesets_file>

The syntax is similar to the ``diff`` command, but the output will be saved to the specified file instead of printed on the console. The two commits are optional, and allow to select the snapshots to compare, with the same meaning as the equivalent parameters in the ``diff`` command.


An OSM workflow using GeoGit
-----------------------------

The following is a short exercise demostrating how GeoGit can be used as part of a workflow involving OSM data.

First, let's initialize the repository.

::

	$ geogit init

For this example, we will be working on a small area around ****. The first step is to get the data corresponding to that area. We will be using a bounding box filtering, which will retrieve all the data within the area, including both ways and nodes.

Run the following command:

::

	$ geogit osm import --bbox 50.79 7.19 50.8 7.20


Your OSM data should now be in your GeoGit repository, and a new commit should have been made.

::

	$ geogit log
	Commit:  58b84cee8f4817b96804324e83d10c31174da695
	Author:  volaya <volaya@opengeo.org>
	Date:    (25 seconds ago) 2013-05-21 12:34:30 +0300
	Subject: Update OSM to changeset 16215593

If you want to edit that data and work on it, you can export it using the ``osm export`` command.

::

	$ geogit osm export exported.xml

You can open the ``exported.xml`` file in a software such as JOSM and edit it. Once it is edited, export it back to an OSM file.

To create a new snapshot in the geogit repository with the edited data, just import the new OSM file

::

	$ geogit osm import editedWithJosm.xml

This will create a new commit with the modified data.

::

	$ geogit log
	Commit: a465736fdabc6d6b5a3289499bba695328a6b43c 	        
	Author:  volaya <volaya@opengeo.org>
	Date:    (15 seconds ago) 2013-05-21 12:37:33 +0300
	Subject: Imported OSM data from editedwithJosm.xml

	Commit:  58b84cee8f4817b96804324e83d10c31174da695
	Author:  volaya <volaya@opengeo.org>
	Date:    (3 minutes ago) 2013-05-21 12:34:30 +0300
	Subject: Update OSM to changeset 16215593

To merge that with the current data in the OSM planet, in case there have been changes, use the ``update`` command.

::

	$ geogit update

If there are conflicts, the operation will be stopped and you should resovle them as usual. If not, the, changes will merged with the changes you just added when importing the xml file. If there are no changes since the last time you fetched data from the OSM server, no commit will be made, and the repository will not be changed by the update operation.


