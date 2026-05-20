package noodle.finance.core.domain

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionMatcherTests {
    fun emailSampleSource() =
        listOf(
            Arguments.of(
                "A transaction of SGD459.62 was debited from your a/c XXXXXX5233 at 05:01PM 6-May-2025, SGT. Ref: Inward DR - GIRO, IRAS, TAXS S9311037D, ITX.",
                -459620,
                "5233",
                "2025-05-06",
                "Inward DR - GIRO, IRAS, TAXS S9311037D, ITX",
            ),
            Arguments.of(
                "A transaction of SGD 51.50 was made with your UOB Card ending 1995 on 02/05/25 at Prudential 18031343. If unauthorised, call 24/7 Fraud Hotline now",
                -51500,
                "1995",
                "2025-05-02",
                "Prudential 18031343",
            ),
            Arguments.of(
                "A transaction of SGD 10.19 was made with your UOB Card ending 1995 on 01/05/25 at GIGA. If unauthorised, call 24/7 Fraud Hotline now",
                -10190,
                "1995",
                "2025-05-01",
                "GIGA",
            ),
            Arguments.of(
                "You made a NETS QR payment of SGD 2.80 to CHING CHING CO on your a/c ending 5233 at 10:38AM SGT, 02 May 25.",
                -2800,
                "5233",
                "2025-05-02",
                "CHING CHING CO",
            ),
            Arguments.of(
                "You made a NETS QR payment of SGD 10.40 to GRANDPA CHEUNG on your a/c ending 5233 at 10:33AM SGT, 02 May 25.",
                -10400,
                "5233",
                "2025-05-02",
                "GRANDPA CHEUNG",
            ),
            Arguments.of(
                "You made/scheduled a funds transfer(s) of SGD 0.01 to DBS BANK LTD a/c ending 3160 from your a/c ending 5233 at 9:22PM SGT, 3 May 25.",
                -10,
                "5233",
                "2025-05-03",
                "DBS BANK LTD a/c ending 3160",
            ),
            Arguments.of(
                "You made a PayNow transfer of SGD 14.00 to LAM XIN YI, JASMINE (Mobile ending 2129) on your a/c ending 5233 at 8:27PM SGT, 2 May 25.",
                -14000,
                "5233",
                "2025-05-02",
                "LAM XIN YI, JASMINE (Mobile ending 2129)",
            ),
            Arguments.of(
                "You made a PayNow transfer of SGD 0.01 to LAM XIN YI, JASMINE (Mobile ending 2129) on your a/c ending 5233 at 8:27PM SGT, 10 May 25.",
                -10,
                "5233",
                "2025-05-10",
                "LAM XIN YI, JASMINE (Mobile ending 2129)",
            ),
            Arguments.of(
                "Alerts May 08 2025, 06:43 AM Transaction of +SGD 3.78 was made on your credit card ****1060 on 08-May-25 06:43 AM at BUS/MRT.",
                -3780,
                "1060",
                "2025-05-08",
                "BUS/MRT",
            ),
            Arguments.of(
                "Alerts April 19 2025, 05:54 AM Dear Valued Customer, Transaction Alert Primary / Supplementary Card Alert Thank you for charging +SGD 3.54 on 19-Apr-25 05:54 AM to yr credit card ****1060 at BUS/MRT.",
                -3540,
                "1060",
                "2025-04-19",
                "BUS/MRT",
            ),
            Arguments.of(
                "Alerts April 16 2025, 10:08 PM Dear Valued Customer, Transaction Alert Primary / Supplementary Card Alert Thank you for charging +SGD 8.30 to your credit card 1060 at McDonalds 930170.",
                -8300,
                "1060",
                "2025-04-16",
                "McDonalds 930170",
            ),
            Arguments.of(
                "Date & Time: 25 Aug 16:21 (SGT) Amount: SGD2.20 From: Shared Expenses A/C ending 3160 To: noodle (MOBILE ending 3336) If unauthorised, please call our DBS hotline.",
                -2200,
                "3160",
                "${LocalDate.now().year}-08-25",
                "noodle (MOBILE ending 3336)",
            ),
        )

    private val configuration = getResource("/application.yml")?.readYaml<Configuration>()

    @ParameterizedTest
    @MethodSource("emailSampleSource")
    fun configuredPatterns(
        input: String,
        amount: Int,
        account: String,
        date: String,
        payee: String,
    ) {
        val matchers = configuration?.matchers?.map(::TransactionMatcher) ?: emptyList()

        val transaction = matchers.firstNotNullOfOrNull { it.parse(input) }

        assertEquals(amount, transaction?.amount)
        assertEquals(account, transaction?.accountId)
        assertEquals(date, transaction?.date)
        assertEquals(payee, transaction?.payeeName)
    }
}
