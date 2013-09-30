/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.freeside.betamax

import javax.net.ssl.HttpsURLConnection
import co.freeside.betamax.encoding.GzipEncoder
import org.junit.Rule
import spock.lang.*
import static java.net.HttpURLConnection.HTTP_OK
import static org.apache.http.HttpHeaders.VIA

@Unroll
class SmokeSpec extends Specification {

	@Rule
	Recorder recorder = new ProxyRecorder(sslSupport: true)

	@Betamax(tape = 'smoke spec')
	void '#type response data'() {
		when:
		HttpURLConnection connection = uri.toURL().openConnection()
		connection.setRequestProperty("Accept-Encoding", "gzip")

		then:
		connection.responseCode == HTTP_OK
		connection.getHeaderField(VIA) == "Betamax"
		connection.inputStream.text.contains(expectedContent)

		where:
		type   | uri                             | expectedContent
		'txt'  | 'http://httpbin.org/robots.txt' | 'User-agent: *'
		'html' | 'http://httpbin.org/html'       | '<!DOCTYPE html>'
		'json' | 'http://httpbin.org/get'        | '"url": "http://httpbin.org/get"'
	}

	@Betamax(tape = 'smoke spec')
	void 'gzipped response data'() {
		when:
		HttpURLConnection connection = uri.toURL().openConnection()
		connection.setRequestProperty("Accept-Encoding", "gzip")

		then:
		connection.responseCode == HTTP_OK
		connection.getHeaderField(VIA) == 'Betamax'
		encoder.decode(connection.inputStream).contains('"gzipped": true')

		where:
		uri = 'http://httpbin.org/gzip'
		encoder = new GzipEncoder()
	}

	@Betamax(tape = 'smoke spec')
	void 'redirects are followed'() {
		when:
		HttpURLConnection connection = uri.toURL().openConnection()

		then:
		connection.responseCode == HTTP_OK
		connection.getHeaderField(VIA) == 'Betamax'
		connection.inputStream.text.contains('"url": "http://httpbin.org/get"')

		where:
		uri = 'http://httpbin.org/redirect/1'
	}

	@Betamax(tape = 'smoke spec')
	void 'https proxying'() {
		when:
		HttpsURLConnection connection = uri.toURL().openConnection()

		then:
		connection.responseCode == HTTP_OK
		connection.inputStream.text.contains('"url": "http://httpbin.org/get"')
		connection.getHeaderField(VIA) == 'Betamax'

		where:
		uri = 'https://httpbin.org/get'
	}

	@Betamax(tape = 'smoke spec')
	void 'can POST to https'() {
		when:
		HttpsURLConnection connection = uri.toURL().openConnection()
		connection.requestMethod = "POST"
		connection.doOutput = true
		connection.outputStream.withStream {
			it << 'message=O HAI'
		}

		then:
		connection.responseCode == HTTP_OK
		connection.getHeaderField(VIA) == 'Betamax'
		connection.inputStream.text.contains('"message": "O HAI"')

		where:
		uri = 'https://httpbin.org/post'
	}

	@Ignore("site is no longer there")
	@Issue('https://github.com/robfletcher/betamax/issues/52')
	@Betamax(tape = 'ocsp')
	void 'OCSP messages'() {
		when:
		HttpURLConnection connection = uri.toURL().openConnection()
		connection.requestMethod = "POST"

		then:
		connection.responseCode == HTTP_OK
		connection.getHeaderField(VIA) == 'Betamax'
		connection.inputStream.bytes.length == 2529

		where:
		uri = 'http://ocsp.ocspservice.com/public/ocsp'
	}

	@Issue(['https://github.com/robfletcher/betamax/issues/61', 'http://jira.codehaus.org/browse/JETTY-1533'])
	@Betamax(tape = 'smoke spec')
	void 'can cope with URLs that do not end in a slash'() {
		when:
		HttpURLConnection connection = uri.toURL().openConnection()

		then:
		connection.responseCode == HTTP_OK
		connection.getHeaderField(VIA) == 'Betamax'

		where:
		uri = 'http://httpbin.org'
	}
}
