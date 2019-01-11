from setuptools import setup

dependencies = [
    "python-jose",
    "pika",
    "lxml"
]

setup(name='drawsmb',
      version='0.1',
      description='DRAWS Message Bus API',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['draws', 'draws/messages', 'draws/messages/rabbitmq', 'draws/messages/security', 'draws/messages/configuration'],
      install_requires=dependencies,
      zip_safe=False)
