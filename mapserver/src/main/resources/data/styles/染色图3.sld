<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
  <NamedLayer>
    <Name>gtopo</Name>
    <UserStyle>
      <Name>dem</Name>
      <Title>Simple DEM style</Title>
      <Abstract>Classic elevation color progression</Abstract>
      <FeatureTypeStyle>
        <Rule>
          <RasterSymbolizer>
            <Opacity>1.0</Opacity>
            <ColorMap>
              <ColorMapEntry color="#7bdcd4" quantity="500"  label="values" />
              <ColorMapEntry color="#98eb9f" quantity="1500" label="values" />
              <ColorMapEntry color="#ffffc7" quantity="2000" label="values" />
              <ColorMapEntry color="#ffff80" quantity="2500" label="values" />
              <ColorMapEntry color="#865f25" quantity="3000" label="values" />
              <ColorMapEntry color="#6596b5" quantity="4000" label="values" />
              <ColorMapEntry color="#fcf7fc" quantity="5000" label="values" />
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
