PROJECT LAYOUT
--------------

src --------------------------------------------------------> (source folder)
		|
		|__ main ___ __ config------------------------------> Module configuration.
        |           |
        |           |__ java -------------------------------> Java classes, duh!
        |           |
        |           |__ site-webscripts --------------------> Surf components and data web-scripts
		|			|
		|			|__ site-data --------------------------> Surf model objects
        |           |
        |           |__ templates --------------------------> Surf page templates
        |           |
        |           |__ messages ---------------------------> Message Bundles
		|			|
		|			|__ webapp -----------------------------> Web resources - will be processed by merger / minifer
        |
		|
		target - Project build dir