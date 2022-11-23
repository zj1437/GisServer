<?xml version="1.0" encoding="GB2312"?>
<sld:StyledLayerDescriptor version="1.0.0" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink">
  <sld:NamedLayer>
    <sld:Name>grass</sld:Name>
    <sld:UserStyle>
      <sld:Name>grass</sld:Name>
      <sld:FeatureTypeStyle>
        <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
        <sld:Rule>
          <sld:Name>grass</sld:Name>
          <sld:Title>grass fill</sld:Title>
		   <sld:PolygonSymbolizer>
            <sld:Fill>
              <sld:GraphicFill>
                <sld:Graphic>
                  <sld:ExternalGraphic>
                    <sld:OnlineResource xlink:type="simple" xlink:href="grass_fill.png" />
                    <sld:Format>image/png</sld:Format>
                  </sld:ExternalGraphic>
                  <sld:Opacity>
                    <ogc:Literal>1.0</ogc:Literal>
                  </sld:Opacity>
                </sld:Graphic>
              </sld:GraphicFill>
            </sld:Fill>
            <sld:Stroke>
              <sld:CssParameter name="stroke">#000000</sld:CssParameter>
              <sld:CssParameter name="stroke-width">1</sld:CssParameter>
            </sld:Stroke>
          </sld:PolygonSymbolizer>
        </sld:Rule>
      </sld:FeatureTypeStyle>
    </sld:UserStyle>
  </sld:NamedLayer>
</sld:StyledLayerDescriptor>