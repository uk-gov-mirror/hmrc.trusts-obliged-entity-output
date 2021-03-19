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

package controllers.actions

import com.google.inject.Inject
import models.requests.IdentifierRequest
import models.{URN, UTR}
import play.api.Logging
import play.api.mvc.Results._
import play.api.mvc.{Request, Result, _}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import utils.Session

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class AuthenticatedIdentifierAction @Inject()(identifier: String,
                                              override val authConnector: AuthConnector)
                                             (implicit val parser: BodyParsers.Default,
                                              val executionContext: ExecutionContext)
  extends IdentifierAction with AuthorisedFunctions with Logging {

  def invokeBlock[A](request: Request[A],
                     block: IdentifierRequest[A] => Future[Result]) : Future[Result] = {

    import TrustEnrolments._

    val retrievals =
      Retrievals.internalId and
      Retrievals.affinityGroup and
      Retrievals.allEnrolments

    implicit val hc : HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

    (identifier match {
      case Identifiers.utrPattern(_) => Some(UTR(identifier))
      case Identifiers.urnPattern(_) => Some(URN(identifier))
      case _ => None
    }) match {
      case Some(value) =>
        authorised().retrieve(retrievals) {
          case Some(internalId) ~ Some(affinity) ~ enrolments if enrolments.trustIdentifier.contains(identifier) =>
            block(IdentifierRequest(request, internalId, value, Session.id(hc), affinity))
          case _ =>
            logger.info(s"[Session ID: ${Session.id(hc)}] Insufficient enrolment")
            Future.successful(Unauthorized)
        } recoverWith {
          case e: AuthorisationException =>
            logger.info(s"[Session ID: ${Session.id(hc)}] AuthorisationException: $e")
            Future.successful(Unauthorized)
        }
      case None =>
        Future.successful(Unauthorized)
    }
  }
}



trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent]

object Identifiers {
  val utrPattern: Regex = "^([0-9]){10}$".r
  val urnPattern: Regex = "^([A-Z0-9]){15}$".r
}