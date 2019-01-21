<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="text"/>
	<xsl:variable name="className" select="/Message/@name"/>
	<xsl:variable name="objTypes" select="'String'" />
	<xsl:template match="/Message">
		<xsl:text>package </xsl:text><xsl:value-of select="@package"/><xsl:text>;

import alma.icd.adapt.messagebus.AbstractMessage;

</xsl:text>
		<xsl:text>public class </xsl:text><xsl:value-of select="$className"/><xsl:text> extends AbstractMessage {
</xsl:text><xsl:apply-templates select="entry" mode="genVars"/><xsl:text>
    public </xsl:text><xsl:value-of select="$className"/><xsl:text>(</xsl:text><xsl:apply-templates select="entry" mode="genParams"/><xsl:text>) {
</xsl:text><xsl:apply-templates select="entry" mode="genAssign"/>
		<xsl:text>    }

    public boolean equals(Object obj) {
        </xsl:text><xsl:value-of select="@name"/><xsl:text> msg = (</xsl:text><xsl:value-of select="@name"/><xsl:text>) obj;
        if (this == msg)
            return true;
        if (msg == null)
            return false;
        if (this.getClass() != msg.getClass())
            return false;
</xsl:text><xsl:apply-templates select="entry" mode="genEquals"/>
		<xsl:text>        return true;
    }

    public String toString() {
        return this.getClass().getName() + "[" + </xsl:text><xsl:apply-templates select="entry" mode="genStr"/><xsl:text>"]";
    }
}
</xsl:text>
	</xsl:template>

	<xsl:template match="entry" mode="genParams">
		<xsl:text xml:space="preserve"/>
		<xsl:value-of select="@type"/>
		<xsl:text> </xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:if test="position() != last( )">, </xsl:if>
	</xsl:template>

	<xsl:template match="entry" mode="genAssign">
		<xsl:text xml:space="preserve"/>
		<xsl:text>        this.</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text> = </xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text>;
</xsl:text>
	</xsl:template>

	<xsl:template match="entry" mode="genVars">
		<xsl:text xml:space="preserve"/>
		<xsl:text>    public </xsl:text>
		<xsl:value-of select="@type"/>
		<xsl:text> </xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text>;
</xsl:text>
	</xsl:template>

	<xsl:template match="entry" mode="genEquals">
		<xsl:variable name="type" select="@type"/>
		<xsl:text xml:space="preserve"/>
		<xsl:text>        if (this.</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:if test="contains(concat(' ', $objTypes, ' '), concat(' ', $type, ' ' ))">
		<xsl:text> == null &amp;&amp; msg.</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text> != null)
            return false;
        if (this.</xsl:text>
		</xsl:if>
		<xsl:if test="contains(concat(' ', $objTypes, ' '), concat(' ', $type, ' ' ))">
		<xsl:value-of select="@name"/>
		<xsl:text> != null &amp;&amp; this.</xsl:text>
		<xsl:value-of select="@name"/>
		</xsl:if>
		<xsl:text> != msg.</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text>)
            return false;
</xsl:text>
	</xsl:template>

	<xsl:template match="entry" mode="genStr">
		<xsl:text xml:space="preserve"/>
		<xsl:text>"</xsl:text>
		<xsl:if test="position() != 1">, </xsl:if>
		<xsl:value-of select="@name"/>
		<xsl:text>=" + this.</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text> + </xsl:text>		
	</xsl:template>

</xsl:stylesheet>
