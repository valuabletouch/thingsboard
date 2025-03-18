##### Examples

* Display styled label with corresponding latest telemetry data for `energy meter` or `thermometer` device types:

```javascript
var deviceType = data.Type;
if (typeof deviceType !== undefined) {
  if (deviceType == "energy meter") {
    return '<span style="color:orange;">${entityName}, ${energy:2} kWt</span>';
  } else if (deviceType == "thermometer") {
    return '<span style="color:blue;">${entityName}, ${temperature:2} °C</span>';
  }
}
return data.entityName;
{:copy-code}
```

<br>
<br>
