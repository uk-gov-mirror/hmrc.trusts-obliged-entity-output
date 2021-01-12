/*
 * Copyright 2021 HM Revenue & Customs
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

import config.Constants._
import controllers.Assets.AUTHORIZATION
import helpers.ConnectorSpecHelper
import helpers.JsonHelper._
import models.{BadRequestTrustDataResponse, _}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class TrustDataConnectorSpec extends ConnectorSpecHelper {

  private lazy val connector: TrustDataConnector = injector.instanceOf[TrustDataConnector]

  private val utrIdentifier: Identifier = UTR("2134514321")
  private val urnIdentifier: Identifier = URN("XATRUST80000001")
  private val invalidIdentifier: Identifier = UTR("1234567890abcdefg")

  private def url(identifier: Identifier): String = s"/trusts/obliged-entities/$identifier/${identifier.value}"

  private val json: JsValue = getJsonValueFromFile("nrs-request-body.json")

  private implicit val ec: ExecutionContext = ExecutionContext.global

  "TrustDataConnector" when {

    ".getTrustJson" must {

      "return a SuccessfulTrustDataResponse with a Json payload" when {

        lazy val headers: Seq[(String, String)] = Seq(
          ENVIRONMENT -> appConfig.trustDataEnvironment,
          CORRELATION_ID -> fakeCorrelationId,
          AUTHORIZATION -> fakeBearerToken
        )

        implicit lazy val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headers)

        "a valid UTR is sent" in {

          stubForGet(url = url(utrIdentifier), requestHeaders = headers, responseStatus = OK, responseBody = Json.stringify(json))

          whenReady(connector.getTrustJson(utrIdentifier)) {
            response =>
              response mustBe SuccessfulTrustDataResponse(json)
          }
        }

        "a valid URN is sent" in {

          stubForGet(url = url(urnIdentifier), requestHeaders = headers, responseStatus = OK, responseBody = Json.stringify(json))

          whenReady(connector.getTrustJson(urnIdentifier)) {
            response =>
              response mustBe SuccessfulTrustDataResponse(json)
          }
        }
      }

      "return BadRequestTrustDataResponse" when {
        "an invalid identifier is sent" in {

          stubForGet(url = url(invalidIdentifier), responseStatus = BAD_REQUEST, responseBody = "")

          whenReady(connector.getTrustJson(invalidIdentifier)) {
            response =>
              response mustBe BadRequestTrustDataResponse
          }
        }
      }

      "return UnprocessableEntityTrustDataResponse" when {
        "422 (UNPROCESSABLE_ENTITY) response received" in {

          stubForGet(url = url(utrIdentifier), responseStatus = UNPROCESSABLE_ENTITY, responseBody = "")

          whenReady(connector.getTrustJson(utrIdentifier)) {
            response =>
              response mustBe UnprocessableEntityTrustDataResponse
          }
        }
      }

      "return ServiceUnavailableTrustDataResponse" when {
        "503 (SERVICE_UNAVAILABLE) response received" in {

          stubForGet(url = url(utrIdentifier), responseStatus = SERVICE_UNAVAILABLE, responseBody = "")

          whenReady(connector.getTrustJson(utrIdentifier)) {
            response =>
              response mustBe ServiceUnavailableTrustDataResponse
          }
        }
      }

      "return UnauthorisedTrustDataResponse" when {
        "401 (UNAUTHORISED) response received" when {
          "authorization header missing" in {

            lazy val headers: Seq[(String, String)] = Seq(
              ENVIRONMENT -> appConfig.trustDataEnvironment,
              CORRELATION_ID -> fakeCorrelationId
            )

            implicit lazy val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headers)

            stubForGet(url = url(utrIdentifier), requestHeaders = headers, responseStatus = UNAUTHORIZED, responseBody = "")

            whenReady(connector.getTrustJson(utrIdentifier)) {
              response =>
                response mustBe UnauthorisedTrustDataResponse
            }
          }
        }
      }

      "return ForbiddenTrustDataResponse" when {
        "403 (FORBIDDEN) response received" when {
          "environment and authorization headers missing" in {

            lazy val headers: Seq[(String, String)] = Seq(
              CORRELATION_ID -> fakeCorrelationId
            )

            implicit lazy val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headers)

            stubForGet(url = url(utrIdentifier), requestHeaders = headers, responseStatus = FORBIDDEN, responseBody = "")

            whenReady(connector.getTrustJson(utrIdentifier)) {
              response =>
                response mustBe ForbiddenTrustDataResponse
            }
          }

          "environment header missing" in {

            lazy val headers: Seq[(String, String)] = Seq(
              CORRELATION_ID -> fakeCorrelationId,
              AUTHORIZATION -> fakeBearerToken
            )

            implicit lazy val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headers)

            stubForGet(url = url(utrIdentifier), requestHeaders = headers, responseStatus = FORBIDDEN, responseBody = "")

            whenReady(connector.getTrustJson(utrIdentifier)) {
              response =>
                response mustBe ForbiddenTrustDataResponse
            }
          }

          "correlation ID header missing" in {

            lazy val headers: Seq[(String, String)] = Seq(
              ENVIRONMENT -> appConfig.trustDataEnvironment,
              AUTHORIZATION -> fakeBearerToken
            )

            implicit lazy val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headers)

            stubForGet(url = url(utrIdentifier), requestHeaders = headers, responseStatus = FORBIDDEN, responseBody = "")

            whenReady(connector.getTrustJson(utrIdentifier)) {
              response =>
                response mustBe ForbiddenTrustDataResponse
            }
          }
        }
      }

      "return NotFoundTrustDataResponse" when {
        "404 (NOT_FOUND) response received" in {

          stubForGet(url = url(utrIdentifier), responseStatus = NOT_FOUND, responseBody = "")

          whenReady(connector.getTrustJson(utrIdentifier)) {
            response =>
              response mustBe NotFoundTrustDataResponse
          }
        }
      }

      "return InternalServerErrorTrustDataResponse" when {
        "500 response received" in {

          stubForGet(url = url(utrIdentifier), responseStatus = INTERNAL_SERVER_ERROR, responseBody = "")

          whenReady(connector.getTrustJson(utrIdentifier)) {
            response =>
              response mustBe InternalServerErrorTrustDataResponse
          }
        }
      }
    }
  }
}
