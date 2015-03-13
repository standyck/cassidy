<?xml version="1.0" encoding="utf-8"?>
<!--
$Id: Identity.xslt 15 2010-09-17 06:17:09Z stand $

Description: This is the standard identity stylesheet
taken from the xslt spec.

-->

<xsl:stylesheet
	 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	 version="2.0"
	 >

  <xsl:output
		method="xml"
		encoding="utf-8"
		indent="yes"
		/>

  <!--Identity Stylesheet -->

  <xsl:template match="@*|node()">
	 <xsl:copy>
		<xsl:apply-templates select="@*|node()"/>
	 </xsl:copy>
  </xsl:template>
</xsl:stylesheet>