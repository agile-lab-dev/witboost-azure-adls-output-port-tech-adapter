{{/*
Expand the name of the chart.
*/}}
{{- define "adlsopspname" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "adlsopspfullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "adlsopspchart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "adlsopsplabels" -}}
app.kubernetes.io/name: {{ include "adlsopspname" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: javasp
app.kubernetes.io/part-of: witboost
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ include "adlsopspchart" . }}
{{- if .Values.labels }}
{{ toYaml .Values.labels }}
{{- else if and .Values.global .Values.global.labels }}
{{ toYaml .Values.global.labels }}
{{- end }}
{{- end -}}

