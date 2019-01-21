<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="text"/>
	<xsl:variable name="className" select="/Message/@name"/>
	<xsl:template match="/Message">
		<xsl:text>from adapt.messagebus.AbstractMessage import AbstractMessage

</xsl:text>
		<xsl:text>class </xsl:text><xsl:value-of select="$className"/><xsl:text>(AbstractMessage):
    def __init__(self, </xsl:text><xsl:apply-templates select="entry" mode="genParams"/><xsl:text>):
</xsl:text><xsl:apply-templates select="entry" mode="genAssign"/>
		<xsl:text>    def serialize(self):
        ret = super(</xsl:text><xsl:value-of select="$className"/><xsl:text>).serialize()
        return ret
    def deserialize(self, dct):
        super(</xsl:text><xsl:value-of select="$className"/><xsl:text>).deserialize(dct)
    def __eq__(self, obj):
        if self is obj:
            return True
        if obj is None:
            return False
        if self.__class__ != obj.__class__:
            return False
</xsl:text><xsl:apply-templates select="entry" mode="genEquals"/>
		<xsl:text>        return True
    def __str__(self):
        return self.__class__.__name__ + "[" + </xsl:text><xsl:apply-templates select="entry" mode="genStr"/><xsl:text>"]"
</xsl:text>
	</xsl:template>

	<xsl:template match="entry" mode="genParams">
		<xsl:text xml:space="preserve"/>
		<xsl:value-of select="@name"/>
		<xsl:text>=None</xsl:text>
		<xsl:if test="position() != last( )">, </xsl:if>
	</xsl:template>

	<xsl:template match="entry" mode="genAssign">
		<xsl:text xml:space="preserve"/>
		<xsl:text>        self.</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text> = </xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text>
</xsl:text>
	</xsl:template>


	<xsl:template match="entry" mode="genEquals">
		<xsl:text xml:space="preserve"/>
		<xsl:text>        if self.</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text> is None and obj.</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text> is not None:
            return False
        if self.</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text> is not None and not self.</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text> == obj.</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text>:
            return False
</xsl:text>
	</xsl:template>

	<xsl:template match="entry" mode="genStr">
		<xsl:text xml:space="preserve"/>
		<xsl:text>"</xsl:text>
		<xsl:if test="position() != 1">, </xsl:if>
		<xsl:value-of select="@name"/>
		<xsl:text>=" + str(self.</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text>) + </xsl:text>		
	</xsl:template>

</xsl:stylesheet>
