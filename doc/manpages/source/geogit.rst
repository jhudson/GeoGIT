.. geogit:

NAME
####


SYNOPSIS
********
::

    geogit [--version] [--help] [--dir=<path>] [--namespace=<name>] <command> [<args>]


DESCRIPTION
***********

GeoGIT is a distributed revision control system for geographic information, written in Java and inspired by the GIT distributed revsion control system's design and principles, adapting them to the differences in the problem domain, but having absolutely no relation with the GIT project.


OPTIONS
*******

--version      Prints the program version number.

--help         Prints the synopsis and a list of the most commonly used commands.

               If the option ``--all`` or ``-a`` is given then all available commands are printed.


               If a ``geogit`` command is named this option will bring up the manual page for that command.


EXAMPLES
********
::

   geogit --help
   geogit help clone 


SEE ALSO
********

 geogit-init


