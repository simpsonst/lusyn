## Copyright 2021, Lancaster University
## All rights reserved.
## 
## Redistribution and use in source and binary forms, with or without
## modification, are permitted provided that the following conditions are
## met:
## 
##  * Redistributions of source code must retain the above copyright
##    notice, this list of conditions and the following disclaimer.
## 
##  * Redistributions in binary form must reproduce the above copyright
##    notice, this list of conditions and the following disclaimer in the
##    documentation and/or other materials provided with the
##    distribution.
## 
##  * Neither the name of the copyright holder nor the names of its
##    contributors may be used to endorse or promote products derived
##    from this software without specific prior written permission.
## 
## THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
## "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
## LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
## A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
## OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
## SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
## LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
## DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
## THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
## (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
## OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
## 
## 
## Author: Steven Simpson <https://github.com/simpsonst>

all::

SED=sed
FIND=find
XARGS=xargs
PREFIX=/usr/local

VWORDS:=$(shell src/getversion.sh --prefix=v MAJOR MINOR PATCH)
VERSION:=$(word 1,$(VWORDS))
BUILD:=$(word 2,$(VWORDS))

## Provide a version of $(abspath) that can cope with spaces in the
## current directory.
myblank:=
myspace:=$(myblank) $(myblank)
MYCURDIR:=$(subst $(myspace),\$(myspace),$(CURDIR)/)
MYABSPATH=$(foreach f,$1,$(if $(patsubst /%,,$f),$(MYCURDIR)$f,$f))

-include $(call MYABSPATH,config.mk)
-include lusyn-env.mk

JARDEPS_SRCDIR=src/java/tree
JARDEPS_MERGEDIR=src/java/merge

SELECTED_JARS += lusyn
jars += lusyn
trees_lusyn += core
roots_core=$(found_core)

SELECTED_JARS += lusyn_aproc
jars += lusyn_aproc
trees_lusyn_aproc += aproc
roots_aproc=$(found_aproc)
deps_aproc += core

include jardeps.mk
-include jardeps-install.mk

DOC_PKGS += uk.ac.lancs.syntax

DOC_OVERVIEW=src/java/overview.html
DOC_CLASSPATH += $(jars:%=$(JARDEPS_OUTDIR)/%.jar)
DOC_SRC=$(call jardeps_srcdirs4jars,$(jars))
DOC_CORE=lusyn

install:: install-jars

install-jars:: $(SELECTED_JARS:%=install-jar-%)
install-jar-%::
	@$(call JARDEPS_INSTALL,$(PREFIX)/share/java,$*,$(version_$*))


blank:: clean

clean:: tidy

tidy::
	@$(PRINTF) 'Removing detritus\n'
	@$(FIND) . -name "*~" -delete

distclean:: blank
	$(RM) BUILD VERSION

all:: BUILD VERSION installed-jars
installed-jars:: $(SELECTED_JARS:%=$(JARDEPS_OUTDIR)/%.jar)
installed-jars:: $(SELECTED_JARS:%=$(JARDEPS_OUTDIR)/%-src.zip)

MYCMPCP=$(CMP) -s '$1' '$2' || $(CP) '$1' '$2'
.PHONY: prepare-version
mktmp:
	@$(MKDIR) tmp/
prepare-version: mktmp
	$(file >tmp/BUILD,$(BUILD))
	$(file >tmp/VERSION,$(VERSION))
BUILD: prepare-version
	@$(call MYCMPCP,tmp/BUILD,$@)
VERSION: prepare-version
	@$(call MYCMPCP,tmp/VERSION,$@)

YEARS=2021

update-licence:
	$(FIND) . -name '.git' -prune -or -type f -print0 | $(XARGS) -0 \
	$(SED) -i 's/Copyright\s\+[0-9,]\+\sLancaster University/Copyright $(YEARS), Lancaster University/g'
