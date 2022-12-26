# Purpose

This software generates LL(1) parsers.

# Installation

You need [Jardeps](https://github.com/simpsonst/jardeps) to build from source.

```
make
sudo make install
```

To install in a location other than `/usr/local`, override `PREFIX`:

```
make PREFIX=$HOME/.local install
```

You can override defaults by placing them in `config.mk` adjacent to `Makefile`:

```
## In ./config.mk

PREFIX=$(HOME)/.local
```

# Use

Include `lusyn_core.jar` in your class path.
Define an enumeration type, and annotate the members with regular expressions to match tokens, or with simple productions.
Then generate tokenizers and parsers from the enumeration type.

An annotation processor is provided to detect some errors at compile time.
Include `lusyn_aproc.jar` in your processor path to enable it.

[More info available.](https://www.lancaster.ac.uk/~simpsons/software/pkg-lusyn)

# Future

Hmm, probably a better way of doing this would be to have the grammar in a separate file, expressed in a portable format, and then annotate each enumeration constant to match it to a non-terminal or a token described in the file.
