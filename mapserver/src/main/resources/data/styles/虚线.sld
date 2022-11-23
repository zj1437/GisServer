<?xml version="1.0" encoding="GB2312" standalone="yes"?>
<sld:StyledLayerDescriptor version="1.0.0" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink">
  <sld:NamedLayer>
    <sld:Name>虚线</sld:Name>
    <sld:UserStyle>
      <sld:Name>虚线</sld:Name>
      <sld:FeatureTypeStyle>
        <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
        <sld:Rule>
          <sld:Name>name</sld:Name>
          <sld:Title>title</sld:Title>
          <sld:LineSymbolizer>
            <sld:Stroke>
              <sld:CssParameter name="stroke">#000000</sld:CssParameter>
              <sld:CssParameter name="stroke-width">0.4</sld:CssParameter>
              <sld:CssParameter name="stroke-opacity">1</sld:CssParameter>
              <sld:CssParameter name="stroke-dasharray">4 1</sld:CssParameter>
            </sld:Stroke>
          </sld:LineSymbolizer>
        </sld:Rule>
      </sld:FeatureTypeStyle>
    </sld:UserStyle>
  </sld:NamedLayer>
</sld:StyledLayerDescriptor>