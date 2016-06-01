import re
flow.setVariable('target.copy.pathsuffix',False);

org = flow.getVariable('max.org')
app = flow.getVariable('max.app')

org = re.sub(r'[^a-zA-Z0-9_-]',"",org)
app = re.sub(r'[^a-zA-Z0-9_-]',"",app)

org = org.lower()
app = app.lower()

flow.setVariable('max.org',org)
flow.setVariable('max.app',app)