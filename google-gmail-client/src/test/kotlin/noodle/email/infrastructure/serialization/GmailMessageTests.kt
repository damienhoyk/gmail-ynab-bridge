package noodle.email.infrastructure.serialization

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GmailMessageTests {
    @Test
    fun flatten() {
        val parts2 =
            listOf(
                GmailMessage.Part(partId = "2.1", mimeType = "text"),
                GmailMessage.Part(partId = "2.2", mimeType = "text"),
            )

        val parts1 =
            listOf(
                GmailMessage.Part(partId = "1", mimeType = "text"),
                GmailMessage.Part(partId = "2", mimeType = "multipart", parts = parts2),
            )

        val root = GmailMessage.Part(partId = "root", mimeType = "multipart", parts = parts1)
        val flat = root.flatten()

        val expectedOrder = listOf("root", "1", "2", "2.1", "2.2")

        Assertions.assertEquals(5, flat.count())
        Assertions.assertEquals(expectedOrder, flat.map(GmailMessage.Part::partId))
    }

    @Test
    fun clean() {
        val text =
            """
            <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">   <html>   <head>   <meta http-equiv="Content-Type" content="text/html; charset=utf-8">   <title>Alerts CASA</title>   </head>   <body style="font-family:Arial, Helvetica, sans-serif;background:#ebebeb;margin:0;padding:0;">   <table cellpadding="0" cellspacing="0" width="660" align="center" style="border-collapse:collapse; border-left:11px solid #ebebeb;border-right:11px solid #ebebeb;">    <tr>     <td valign="top" style="width:11px;background:#ebebeb;">&nbsp;</td>     <td><table cellpadding="0" cellspacing="0" width="640" align="center" style="border-collapse:collapse;">       <tr>        <td valign="top" style="width:40px;height:11px;background:#ebebeb;" colspan = "3" >&nbsp;</td>       </tr>       <tr>        <td valign="top" colspan="3" style="background:#fbfbfb;"><table cellpadding="0" cellspacing="0" width="640" align="center" style="border-collapse:collapse;">          <tr>           <td width="40" align="left" valign="top" style="height:120px; width:40px;">&nbsp;</td>           <td width="230" align="left" valign="middle" style="height:120px;"> 		 <span style="font-family:Arial, Helvetica, sans-serif;font-size:16pt;color:#6d6d6d;font-weight:normal;margin:0;">Alerts</span><br>            <span style="font-family:Arial, Helvetica, sans-serif;font-size:12pt;color:#6f6f6f;font-weight:normal;margin:0;">April 24 2025, 06:54 AM</span> 		</td>          <td width="328" align="right" valign="middle" style="height:120px;"><a href="#" title="">   		<img src="cid:fce19096-2c20-43cb-be3a-a7f053334dde"  border="0" width="137" height="70" style="width:134px;height:68px;">   		</a></td>           <td width="40" align="right" valign="top" style="height:120px; width:40px;">&nbsp;</td>          </tr>         </table></td>       </tr>       <tr>        <td valign="top" colspan="3" style="background:#fbfbfb;"><img src="cid:92141cf3-8cb8-46fe-bf36-ed0ab973fd26" style="width:640px;height:11px"></td>       </tr>       <tr>        <td valign="top" style="width:40px;height:40px;background:#ffffff;" colspan = "3">&nbsp;</td>       </tr>       <tr>        <td valign="top" style="width:40px;background:#ffffff;">&nbsp;</td>        <td valign="top" style="width:560px;background:#ffffff;"><table cellpadding="0" cellspacing="0" width="560" align="center" style="border-collapse:collapse;">          <tr>           <td><span style="font-family:Arial, Helvetica, sans-serif;display:block;font-size:14pt;color:#009015;font-weight:normal;margin:0;">          Dear Valued Customer, 		</span></td>          </tr>          <tr>           <td style="height:20px; background:#FFFFFF;">&nbsp;</td>          </tr>   	   <tr>   		  <td style="background:FFFFFF;"><span style="font-family:Arial, Helvetica, sans-serif;display:block;font-size:12pt;color:#777777;font-weight:normal;margin:0;line-height:1.45;">Transaction Alert Primary / Supplementary Card Alert</span></td>          </tr>   	   <tr>           <td style="background:FFFFFF;"> 		<span style="font-family:Arial, Helvetica, sans-serif;display:block;font-size:12pt;color:#777777;font-weight:normal;margin:0;line-height:1.45;"> 			Thank you for charging +SGD 5.70 on 24-Apr-25 06:54 AM to yr credit card ****1060 at BUS/MRT. 		 </span></td>          </tr>          <tr>           <td style="height:31px; background:#FFFFFF;">&nbsp;</td>          </tr>     		 		</table></td>        <td valign="top" style="width:40px;background:#ffffff;">&nbsp;</td>       </tr>       <tr>        <td valign="top" style="width:40px;height:25px;background:#ffffff;border-bottom:1px solid #ededed;" colspan = "3">&nbsp;</td>       </tr> 	<tr>        <td valign="top" style="width:40px;height:25px;background:#ffffff;border-bottom:1px solid #ededed;" colspan = "3">&nbsp;</td>       </tr>      <tr>        <td valign="top" style="width:40px;height:30px;background:#ffffff;">&nbsp;</td>        <td valign="top" style="width:560px;height:30px;background:#ffffff;">&nbsp;</td>        <td valign="top" style="width:40px;height:30px;background:#ffffff;">&nbsp;</td>       </tr>       <tr>        <td valign="top" style="width:40px;height:30px;background:#ffffff;">&nbsp;</td>        <td valign="top" style="width:560px;height:30px;background:#ffffff;">&nbsp;</td>        <td valign="top" style="width:40px;height:30px;background:#ffffff;">&nbsp;</td>       </tr>       <tr>        <td valign="top" colspan="3" style="height:8px;background:#ebebeb;">&nbsp;</td>       </tr>       <tr>        <td valign="top" style="width:40px;height:9px;background:#ebebeb;" colspan = "3">&nbsp;</td>       </tr>       <tr>        <td valign="top" style="width:40px;background:#ebebeb;">&nbsp;</td>        <td valign="top" style="width:560px;background:#ebebeb;"><table cellpadding="0" cellspacing="0" width="560" align="center" style="border-collapse:collapse;">          <tr>           <td><span style="font-family:Arial, Helvetica, sans-serif;display:block;font-size:10pt;color:#727272;font-weight:normal;margin:0;line-height:1.5;">To modify these alerts, please logon to your Online Banking account and select Alerts & Notifications. This is a system-generated e-mail and does not require an authorised signature. Please do not reply to the sender of this email.</span></td>          </tr>          <tr>           <td style="height:9px">&nbsp;</td>          </tr>          <tr>           <td> 		<span style="font-family:Arial, Helvetica, sans-serif;display:block;font-size:10pt;color:#727272;font-weight:normal;margin:0;line-height:1.5;">Please note Standard Chartered Bank (Singapore) Limited will never ask you for your account details. To learn more on important legal notices, our data protection and privacy policy and how you can avoid online fraud, please refer to our Privacy Statement and Important Legal Notice Online Terms & Conditions which can be found on the Standard Chartered Bank (Singapore) Limited public website.</span></td>          </tr>         </table></td>        <td valign="top" style="width:40px;background:#ebebeb;">&nbsp;</td>       </tr>       <tr>        <td valign="top" style="width:40px;height:40px;background:#ebebeb;" colspan = "3">&nbsp;</td>       </tr>      </table></td>     <td valign="top" style="width:11px;background:#ebebeb;">&nbsp;</td>    </tr>   </table>   <p>This email and any attachments are confidential and may also be privileged. If you are not the intended recipient, please delete all copies and notify the sender immediately. You may wish to refer to the incorporation details of Standard Chartered PLC, Standard Chartered Bank and their subsidiaries together with Standard Chartered Bank’s Privacy Policy via our public website.<p> </body>   </html>
            """.trimIndent()
                .replace(Regex("<[^>]*>"), "")
                .replace(Regex("\\u00A0|&nbsp;"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

        println(text)
    }
}
