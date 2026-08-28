{{- define "thingworx-operator.name" -}}
{{- .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "thingworx-operator.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "thingworx-operator.name" .) | trunc 63 | trimSuffix "-" }}
{{- end }}
