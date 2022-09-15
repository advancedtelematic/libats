/*
 * Copyright (C) 2016 HERE Global B.V.
 *
 * Licensed under the Mozilla Public License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.mozilla.org/en-US/MPL/2.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: MPL-2.0
 * License-Filename: LICENSE
 */

package com.advancedtelematic.libats.auth

import akka.http.scaladsl.server.{Directive0, Directives}
import com.typesafe.config.ConfigFactory

object Scopes {

  lazy val apiDomain = ConfigFactory.load().getString("scopes.domain")

  lazy val campaigns_str = apiDomain + "campaigns"
  def campaigns(ns: AuthedNamespaceScope) = new ScopeDirectives(ns, campaigns_str)

  lazy val devices_str = apiDomain + "devices"
  def devices(ns: AuthedNamespaceScope) = new ScopeDirectives(ns, devices_str)

  lazy val packages_str = apiDomain + "packages"
  def packages(ns: AuthedNamespaceScope) = new ScopeDirectives(ns, packages_str)

  lazy val resolver_str = apiDomain + "resolver"
  def resolver(ns: AuthedNamespaceScope) = new ScopeDirectives(ns, resolver_str)

  lazy val updates_str = apiDomain + "updates"
  def updates(ns: AuthedNamespaceScope) = new ScopeDirectives(ns, updates_str)
}

class ScopeDirectives(ns: AuthedNamespaceScope, theScope: String) {
  def check: Directive0 = ns.oauthScope(theScope)
  def checkReadonly: Directive0 = ns.oauthScopeReadonly(theScope)
  def delete: Directive0 = Directives.delete & check
  def get: Directive0 = Directives.get & checkReadonly
  def put: Directive0 = Directives.put & check
  def post: Directive0 = Directives.post & check
  def patch: Directive0 = Directives.patch & check
}

