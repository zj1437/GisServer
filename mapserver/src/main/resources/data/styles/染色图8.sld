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
              <ColorMapEntry color="#286d9b" quantity="-8000"  label="values" />
              <ColorMapEntry color="#2b76a8" quantity="-7000" label="values" />
              <ColorMapEntry color="#2f80b5" quantity="-6000" label="values" />
              <ColorMapEntry color="#338bc6" quantity="-5000" label="values" />
              <ColorMapEntry color="#4799cf" quantity="-4000"  label="values" />
              <ColorMapEntry color="#539fd2" quantity="-3000" label="values" />
              <ColorMapEntry color="#5fa6d6" quantity="-2000" label="values" />
              <ColorMapEntry color="#64a9d7" quantity="-1000" label="values" />
              <ColorMapEntry color="#74b3dc" quantity="-500"  label="values" />
              <ColorMapEntry color="#81b9de" quantity="-200" label="values" />
              <ColorMapEntry color="#93c2e2" quantity="-100" label="values" />
              <ColorMapEntry color="#9dc9e6" quantity="-50" label="values" />
              <ColorMapEntry color="#a9cfe9" quantity="-20"  label="values" />
              <ColorMapEntry color="#b7d7ec" quantity="-10" label="values" />
              <ColorMapEntry color="#c9e1f1" quantity="-5" label="values" />
              <ColorMapEntry color="#d0e5f3" quantity="-1" label="values" />
              <ColorMapEntry color="#b9a984" quantity="0"  label="values" />
              <ColorMapEntry color="#9c875a" quantity="5" label="values" />
              <ColorMapEntry color="#65573a" quantity="10" label="values" />
              <ColorMapEntry color="#72603c" quantity="15" label="values" />
              <ColorMapEntry color="#806641" quantity="50"  label="values" />
              <ColorMapEntry color="#866e3e" quantity="200" label="values" />
              <ColorMapEntry color="#b5a873" quantity="600" label="values" />
              <ColorMapEntry color="#aa8c5a" quantity="1000" label="values" />
              <ColorMapEntry color="#a0774b" quantity="2000" label="values" />
			  <ColorMapEntry color="#8c5431" quantity="3000" label="values" />
              <ColorMapEntry color="#662d1c" quantity="4000" label="values" />
              <ColorMapEntry color="#522b24" quantity="5000" label="values" />
              <ColorMapEntry color="#e9e9e9" quantity="6000" label="values" />
              <ColorMapEntry color="#ffffff" quantity="7000" label="values" />
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
