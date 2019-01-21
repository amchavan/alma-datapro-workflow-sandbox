from setuptools import setup

dependencies = [
    "python-jose",
    "pika",
    "lxml"
]

setup(name='adaptmb',
      version='0.1',
      description='ADAPT Python Message Bus API',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['adapt', 'adapt/messagebus', 'adapt/messagebus/rabbitmq', 'adapt/messagebus/security', 'adapt/messagebus/configuration'],
      install_requires=dependencies,
      zip_safe=False)
