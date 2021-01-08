/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import java.util.UUID

import config.AppConfig
import config.Constants._
import controllers.Assets.CONTENT_TYPE
import javax.inject.Inject
import models.{Identifier, TrustDataResponse}
import play.api.http.HeaderNames
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class TrustDataConnector @Inject()(http: HttpClient, config: AppConfig) {

  def getTrustJson(identifier: Identifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[TrustDataResponse] = {
    lazy val url: String = s"${config.trustDataUrl}/trusts/obliged-entities/$identifier/${identifier.value}"

    lazy val headers: Seq[(String, String)] = {
      Seq(
        HeaderNames.AUTHORIZATION -> s"Bearer ${config.trustDataToken}",
        CONTENT_TYPE -> CONTENT_TYPE_JSON,
        ENVIRONMENT -> config.trustDataEnvironment,
        CORRELATION_ID -> UUID.randomUUID().toString
      )
    }

    val hcExtra: HeaderCarrier = hc.withExtraHeaders(headers: _*)

    http.GET[TrustDataResponse](url)(TrustDataResponse.httpReads, hcExtra, ec)
  }

}
