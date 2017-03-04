package net.mikolak.pomisos.prefs

import org.scalatest.{FlatSpec, MustMatchers}

class TrelloPrefsControllerSpec extends FlatSpec with MustMatchers {

  import org.scalatest.OptionValues._

  val tested = TrelloPrefsControllerUtils.extractAuthText _

  "The document extractor" must "extract the authcode from the confirmation page" in {

    val authToken = "33333333333333333333333333333333"

    val validPage = <html>
      <BODY>
        <p>
          You have granted  access to your Trello information.
        </p>
        <p>
          To complete the process, please give  this token:
        </p>
        <pre>
          {authToken}
        </pre>
      </BODY>
    </html>

    tested(validPage).value must be(authToken)
  }

  it must "fall through on previous auth pages" in {
    val previousPage = <html lang="en">
        <head>
           <title>Authorize | Trello</title>
                <link rel="stylesheet" href="/css/account.css"/>
                  <!-- Google Analytics -->
    </head>
    <body class="account-page request-token">
      <div id="surface">

        <div class="account-header compact">
          <img alt="Trello logo" src="/images/logo-blue-lg.png"/>
          </div>

          <div class="account-content clearfix">

            <h1>
              Let <span class="identifier">pomisos</span> use your account?
            </h1>


            <div class="buttons">
              <form method="POST" action="/1/token/approve">
                <input class="primary" type="submit" name="approve" value="Allow"/>
                  <input type="submit" class="deny" value="Deny"/>
                    <input type="hidden" name="requestKey" value="ablablabla"/>
                      <input type="hidden" name="signature" value="1234/hueuhue"/>
                      </form>
                    </div>

                    <hr/>

                      <p class="bottom">
                        You are logged in as <b>M K (mkx320)</b>.

                        The app will be able to use your account <b>until you disable it.</b>
                      </p>

                      <p><b>pomisos</b> is not affiliated with Trello in any way, and by permitting access to your content you assume all related risks and liabilities.</p>

                      <hr/>

                        <div class="allowed">
                          <b>The app will be able to:</b>
                          <ul>
                            <li>Read all of your boards and teams</li>
                            <li>Create and update cards, lists, boards and teams</li>
                            <li>Make comments for you</li>
                          </ul>
                        </div>

                        <div class="disallowed">
                          <b>It won’t be able to:</b>
                          <ul>
                            <li>Read your email address</li>
                            <li>See your Trello password</li>
                          </ul>
                        </div>
      </div>
        </div>
    </body>
    </html>

    tested(previousPage) must be('empty)

  }

}
