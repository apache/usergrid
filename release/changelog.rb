#!/usr/bin/env ruby
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Updates the CHANGELOG for the specified Apache Usergrid release, if no
# version override is specified then .usergridversion will be read and used.
# The version string will need to match the Apache Usergrid jira tickets
# fixVersion to generate this list.
#
require 'rubygems'
require 'json'
require 'uri'
require 'net/http'
require 'net/https'
require 'openssl'
require 'tempfile'

def http_post(uri, params={}, headers={}, body=nil, debug=false)
  http = Net::HTTP.new(uri.host, uri.port)
  http.set_debug_output($stdout) if debug
  if uri.scheme == "https" || uri.port == 443
    http.use_ssl = true
    http.verify_mode = OpenSSL::SSL::VERIFY_NONE
  end
  response = nil

  begin
    request = Net::HTTP::Post.new(uri.request_uri)
    request.set_form_data(params) if (params && params.size > 1)
    request.basic_auth uri.user, uri.password if uri.user
    request.body = body if body

    headers.each_pair do |name, value|
      request[name] = value
    end

    response = http.request(request)
  rescue Exception => e
    raise "Error posting to #{uri.to_s}. #{e}"
  end

  response
end

def get_jira_issues_for_query(url, jql, startAt=0, maxResults=100)
  begin
    request_body = { :jql => jql, :startAt => startAt, :maxResults => maxResults}
    response = http_post(
      URI(url),
      {},
      {"content-type" => 'application/json', "accept" => "application/json"},
      request_body.to_json
    )
  rescue StandardError => e
    raise "Error executing jql query: #{jql}. #{e}"
  end

  begin
    results = JSON.parse(response.body)
  rescue StandardError => e
    raise "Error parsing json result"
  end

  results
end

def get_all_jira_issues_for_query(url, jql)
  startAt = 0
  maxResults = 100

  results = []
  begin
    res = get_jira_issues_for_query(url, jql, startAt, maxResults)
    results.concat(res['issues']) if res && res.has_key?('issues')
    startAt = startAt + maxResults
  end until (results.size() == res['total'] )

  results
end

base_dir = `git rev-parse --show-toplevel`.strip

# Get the current version from the .usergridversion file if no version override is provided
version = nil
if ARGV[0].nil?
  version = nil
  Dir.chdir(base_dir) do
    version = File.read('.usergridversion').strip
  end
else
  version = ARGV[0]
end

raise "Unable to read .usergridversion" if version.nil?

jira_base_url = "https://issues.apache.org/jira"
jira_search_url = "/rest/api/2/search"
jira_url = jira_base_url + jira_search_url

jql="project=AURORA AND fixVersion='#{version}' AND status in (Resolved,Closed) ORDER BY issuetype"

# Fetch all the issues available for the given jql query
results = get_all_jira_issues_for_query(jira_url, jql)

changelog = {}
# Loop through and add all results
results.each do |issue|
  key = issue['key']
  summary = issue['fields']['summary']
  type = issue['fields']['issuetype']['name']

  changelog_entry = "[#{key}] - #{summary}"

  if !changelog[type].nil?
    changelog[type] << changelog_entry
  else
    changelog[type] = [changelog_entry]
  end
end

# Merge the new updates and the existing changelog
tmpfile = Tempfile.open('usergrid.changelog')
begin
  tmpfile.puts "Usergrid #{version}", "-" * 80
  changelog.keys.sort.each do |type|
    tmpfile.puts "## #{type}"
    changelog[type].each { |entry| tmpfile.puts "    * #{entry}" }
    tmpfile.puts ""
  end
  # Append all the existing CHANGELOG entries and write the new CHANGELOG file
  tmpfile.puts ""
  changelog_file = File.join(base_dir, 'CHANGELOG')
  tmpfile.write File.read(changelog_file) if File.exist?(changelog_file)
  tmpfile.rewind
  tmpfile.flush
  File.open(changelog_file,"w+") {|f|  f.write(tmpfile.read) }
ensure
  tmpfile.close
  tmpfile.unlink
end
