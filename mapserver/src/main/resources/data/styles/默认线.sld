<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:se="http://www.opengis.net/se" xmlns:ogc="http://www.opengis.net/ogc" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.1.0/StyledLayerDescriptor.xsd" version="1.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <se:Name>line_shi</se:Name>
    <UserStyle>
      <se:Name>line_shi</se:Name>
      <se:FeatureTypeStyle>
        <se:Rule>
          <se:Name>Single symbol</se:Name>
          <se:LineSymbolizer>
            <se:Stroke>
              <se:CssParameter name="stroke">#7d8c8f</se:CssParameter>
              <se:CssParameter name="stroke-width"><ogc:Literal>1</ogc:Literal></se:CssParameter>
			  <CssParameter name="stroke-linejoin">bevel</CssParameter>
              <CssParameter name="stroke-linecap">square</CssParameter>
            </se:Stroke>
          </se:LineSymbolizer>
        </se:Rule>
      </se:FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
