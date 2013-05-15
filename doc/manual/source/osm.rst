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


Downloaded data is imported into GeoGit and put into two trees: ``way`` and ``node`` with a very basic feature type in both cases . If you want to structure your data differently, you can specify a mapping. This will cause the feature types to be different, created according to the mapping and using the entity tags to fill the corresponding attribute values.

[Mappings are stil not available. When they are implemented, they should be described here]


Unlike the case of importing from a shapefile or database, importing from OSM has to be performed with a clean index and working tree, and the imported data is not just imported into the workign tree, 
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