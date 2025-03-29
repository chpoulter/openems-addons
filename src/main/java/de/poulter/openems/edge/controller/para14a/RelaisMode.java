/*
 *   OpenEMS Meter Paragraph 14a Controller
 *
 *   Written by Christian Poulter.
 *   Copyright (C) 2025 Christian Poulter <devel(at)poulter.de>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *   SPDX-License-Identifier: AGPL-3.0-or-later
 *
 */

package de.poulter.openems.edge.controller.para14a;

// Relaissteuerungsverfahren gemäß VDE FNN, hierbei
// werden 4 Relais benutzt, deren Bedeutung sich
// je nach Verfahren unterschiedlich sein kann.

public enum RelaisMode {
    None,                          // kein Anschluss
    FNN2bit1StbV,                  // FNN2bit für 1 Verbraucher
    FNN2bit2StbV,                  // FNN2bit für 2 Verbraucher
    FNN2bit1StbE,                  // FNN2bit für 1 Erzeuger
    FNN2bit2StbE,                  // FNN2bit für 2 Erzeuger
    FNN2bit1StbV1StbE,             // FNN2bit für 1 Verbraucher und 1 Erzeuger
    DreiRelais1StbE,               // 3 Relais 0%, 30%, 60% für 1 Erzeuger
    VierRelais1StbE,               // 4 Relais 0%, 30%, 60%, 100% für 1 Erzeuger
    Einzelkontakt4StbV             // 4 Relais Einzelkontakte für 4 Verbraucher
    ;
}
