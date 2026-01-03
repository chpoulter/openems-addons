#!/bin/sh
#
#    OpenEMS maven repo
#
#    Written by Christian Poulter.
#    Copyright (C) 2025 Christian Poulter <devel(at)poulter.de>
#
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU Affero General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU Affero General Public License for more details.
#
#    You should have received a copy of the GNU Affero General Public License
#    along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
#    SPDX-License-Identifier: AGPL-3.0-or-later
#

echo Building image
#DOCKER_BUILDKIT=1 docker build -t cpou/openems-maven-repo -f Dockerfile --progress plain --no-cache --pull .
DOCKER_BUILDKIT=1 docker build -t cpou/openems-maven-repo -f Dockerfile --progress plain .

# clear old repo
echo Cleaning old REPO
rm -r ./repo
ls ./repo

# extract repo from docker image
echo Extracting repo from image
id=$(docker create cpou/openems-maven-repo)
docker cp $id:/openems-maven-repo ./repo
docker cp $id:/alljarslist.txt ./alljarslist.txt
docker rm -v $id

echo Result:
find repo -type f -name *jar
